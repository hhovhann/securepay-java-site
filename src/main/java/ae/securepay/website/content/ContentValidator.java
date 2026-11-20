package ae.securepay.website.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rules every content change must pass before it is published (also run by the automated tests on every change).
 * The messages are written so a non-technical editor, or the AI making the change, can see what to fix.
 */
public final class ContentValidator {

    private static final Pattern HTML_TAG = Pattern.compile("<\\s*[a-zA-Z!/]");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]\\n]+)\\]\\(([^)\\s]+)\\)");
    private static final Set<String> URL_KEYS = Set.of("url", "src", "file", "logo", "social_image", "icon_32", "icon_192",
            "apple_touch_icon", "linkedin_url");
    private static final Set<String> GUIDE_MESSAGES = Set.of("removed_sensitive", "sensitive", "out_of_scope", "no_answer",
            "invalid", "rate_limited", "origin_blocked", "unavailable");

    private ContentValidator() {
    }

    /** Page addresses that cannot be used for new pages (application endpoints and files). */
    static final Set<String> RESERVED_SLUGS = Set.of("api", "assets", "robots", "sitemap", "favicon", "web-inf", "meta-inf", "wp-admin", "wp-content");
    static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /**
     * Building blocks for new pages ({@code layout: blocks}): each type and its required fields. They reuse the
     * site's existing design, so a new page needs no code or design work.
     */
    public static final Map<String, List<String>> BLOCKS = Map.of(
            "intro", List.of("title"),
            "image", List.of("src", "alt"),
            "facts", List.of("items"),
            "cards", List.of("title", "cards"),
            "details", List.of("title"),
            "text", List.of("paragraphs"),
            "cta", List.of("title", "link_label", "url"));

    public static List<String> validate(Map<String, Object> site, Map<SitePage, Map<String, Object>> pages,
                                        Map<String, Object> guide, Predicate<String> assetExists) {
        return validate(site, pages, Map.of(), guide, assetExists);
    }

    public static List<String> validate(Map<String, Object> site, Map<SitePage, Map<String, Object>> pages,
                                        Map<String, Map<String, Object>> blockPages, Map<String, Object> guide,
                                        Predicate<String> assetExists) {
        List<String> problems = new ArrayList<>();
        Map<String, Set<String>> anchors = anchors(pages, blockPages);

        require(site, "site.yml", problems, "name", "base_url", "logo", "logo_alt", "menu", "footer");
        if (!String.valueOf(site.get("base_url")).startsWith("https://")) {
            problems.add("site.yml: base_url must start with https://");
        }
        walk("site.yml", null, site, "/", anchors, assetExists, problems);

        for (Map.Entry<SitePage, Map<String, Object>> entry : pages.entrySet()) {
            String file = "pages/" + entry.getKey().slug() + ".yml";
            Map<String, Object> page = entry.getValue();
            Object seo = page.get("seo");
            if (!(seo instanceof Map<?, ?> seoMap) || blank(seoMap.get("title")) || blank(seoMap.get("description"))) {
                problems.add(file + ": seo.title and seo.description are required (they are the search-engine title and summary)");
            } else if (String.valueOf(seoMap.get("description")).length() > 300) {
                problems.add(file + ": seo.description should be at most 300 characters");
            }
            walk(file, null, page, entry.getKey().path(), anchors, assetExists, problems);
        }

        for (Map.Entry<String, Map<String, Object>> entry : blockPages.entrySet()) {
            checkBlockPage(entry.getKey(), entry.getValue(), problems);
            walk("pages/" + entry.getKey() + ".yml", null, entry.getValue(), "/" + entry.getKey() + "/", anchors, assetExists, problems);
        }

        checkHome(pages.get(SitePage.HOME), assetExists, problems);
        checkForm("pages/contact-us.yml", pages.get(SitePage.CONTACT_US), problems);
        checkForm("pages/partners.yml", pages.get(SitePage.PARTNERS), problems);
        checkGuide(guide, problems);
        walk("guide.yml", null, guide, "/", anchors, assetExists, problems);
        return problems;
    }

    private static void walk(String file, String key, Object value, String pagePath, Map<String, Set<String>> anchors,
                             Predicate<String> assetExists, List<String> problems) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> walk(file, key == null ? String.valueOf(k) : key + "." + k, v, pagePath, anchors, assetExists, problems));
        } else if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                walk(file, key + "[" + (i + 1) + "]", list.get(i), pagePath, anchors, assetExists, problems);
            }
        } else if (value instanceof String text) {
            String field = key.contains(".") ? key.substring(key.lastIndexOf('.') + 1) : key;
            field = field.replaceAll("\\[\\d+]$", "");
            if (HTML_TAG.matcher(text).find()) {
                problems.add(file + ": " + key + " contains HTML. Use plain text with the formatting marks in CLAUDE.md.");
            }
            if (URL_KEYS.contains(field)) {
                checkTarget(file, key, text, pagePath, anchors, assetExists, problems);
            }
            Matcher links = LINK.matcher(text);
            while (links.find()) {
                checkTarget(file, key, links.group(2), pagePath, anchors, assetExists, problems);
            }
        }
    }

    private static void checkTarget(String file, String key, String target, String pagePath, Map<String, Set<String>> anchors,
                                    Predicate<String> assetExists, List<String> problems) {
        if (!Html.isAllowedUrl(target)) {
            problems.add(file + ": " + key + " has a link that is not allowed: " + target
                    + " (use a site path like /services/, https://, mailto: or tel:)");
            return;
        }
        if (target.startsWith("#")) {
            if (!anchors.getOrDefault(pagePath, Set.of()).contains(target.substring(1))) {
                problems.add(file + ": " + key + " links to a section that does not exist on this page: " + target);
            }
            return;
        }
        if (!target.startsWith("/")) {
            return;
        }
        if (target.startsWith("/assets/")) {
            if (!assetExists.test(target)) {
                problems.add(file + ": " + key + " refers to a file that is not in the website: " + target);
            }
            return;
        }
        String path = target.contains("#") ? target.substring(0, target.indexOf('#')) : target;
        String anchor = target.contains("#") ? target.substring(target.indexOf('#') + 1) : null;
        if (!anchors.containsKey(path)) {
            problems.add(file + ": " + key + " links to a page that does not exist: " + target);
        } else if (anchor != null && !anchors.getOrDefault(path, Set.of()).contains(anchor)) {
            problems.add(file + ": " + key + " links to a section that does not exist: " + target);
        }
    }

    /** Every page address on the site, with the section anchors (#id) each page has. */
    private static Map<String, Set<String>> anchors(Map<SitePage, Map<String, Object>> pages, Map<String, Map<String, Object>> blockPages) {
        Map<String, Set<String>> anchors = new HashMap<>();
        for (SitePage page : SitePage.values()) {
            anchors.put(page.path(), Set.of());
        }
        for (Map.Entry<String, Map<String, Object>> entry : blockPages.entrySet()) {
            Set<String> ids = new HashSet<>();
            if (entry.getValue().get("blocks") instanceof List<?> blocks) {
                for (Object block : blocks) {
                    if (block instanceof Map<?, ?> map && map.get("id") != null) {
                        ids.add(String.valueOf(map.get("id")));
                    }
                }
            }
            anchors.put("/" + entry.getKey() + "/", ids);
        }
        anchors.put(SitePage.HOME.path(), Set.of("journey"));
        Set<String> serviceIds = new HashSet<>();
        Object sections = pages.getOrDefault(SitePage.SERVICES, Map.of()).get("sections");
        if (sections instanceof List<?> list) {
            for (Object section : list) {
                if (section instanceof Map<?, ?> map && map.get("id") != null) {
                    serviceIds.add(String.valueOf(map.get("id")));
                }
            }
        }
        anchors.put(SitePage.SERVICES.path(), serviceIds);
        return anchors;
    }

    private static void checkBlockPage(String slug, Map<String, Object> page, List<String> problems) {
        String file = "pages/" + slug + ".yml";
        if (!SLUG.matcher(slug).matches() || RESERVED_SLUGS.contains(slug)) {
            problems.add(file + ": the file name becomes the page address /" + slug + "/; use lower-case words joined by dashes (and not "
                    + String.join(", ", RESERVED_SLUGS) + ")");
        }
        if (!"blocks".equals(page.get("layout"))) {
            problems.add(file + ": new pages must start with layout: \"blocks\" (see CLAUDE.md, New pages)");
        }
        Object seo = page.get("seo");
        if (!(seo instanceof Map<?, ?> seoMap) || blank(seoMap.get("title")) || blank(seoMap.get("description"))) {
            problems.add(file + ": seo.title and seo.description are required (they are the search-engine title and summary)");
        }
        if (!(page.get("blocks") instanceof List<?> blocks) || blocks.isEmpty()) {
            problems.add(file + ": blocks must list the page's sections, starting with an intro block");
            return;
        }
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < blocks.size(); i++) {
            String where = file + ": blocks[" + (i + 1) + "]";
            if (!(blocks.get(i) instanceof Map<?, ?> block)) {
                problems.add(where + " must have a type and its fields");
                continue;
            }
            String type = String.valueOf(block.get("type"));
            List<String> required = BLOCKS.get(type);
            if (required == null) {
                problems.add(where + ".type \"" + type + "\" is not available; use one of " + new java.util.TreeSet<>(BLOCKS.keySet()));
                continue;
            }
            for (String field : required) {
                Object value = block.get(field);
                if (value == null || (value instanceof String text && text.isBlank()) || (value instanceof List<?> list && list.isEmpty())) {
                    problems.add(where + " (" + type + ") needs " + field);
                }
            }
            if (block.get("id") != null && (!SLUG.matcher(String.valueOf(block.get("id"))).matches() || !ids.add(String.valueOf(block.get("id"))))) {
                problems.add(where + ".id must be unique lower-case words joined by dashes");
            }
        }
        if (!(blocks.get(0) instanceof Map<?, ?> first) || !"intro".equals(first.get("type"))) {
            problems.add(file + ": the first block must be an intro (it holds the page heading)");
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkHome(Map<String, Object> home, Predicate<String> assetExists, List<String> problems) {
        try {
            Map<String, Object> hero = (Map<String, Object>) home.get("hero");
            Map<String, Object> workflow = (Map<String, Object>) hero.get("workflow");
            Set<String> stages = new LinkedHashSet<>();
            for (Object stage : (List<Object>) workflow.get("stages")) {
                stages.add(String.valueOf(((Map<String, Object>) stage).get("step")));
            }
            Set<String> steps = new LinkedHashSet<>();
            for (Object step : (List<Object>) ((Map<String, Object>) home.get("workflow")).get("steps")) {
                Map<String, Object> s = (Map<String, Object>) step;
                steps.add(String.valueOf(s.get("step")));
                if (blank(s.get("detail_title")) || blank(s.get("detail"))) {
                    problems.add("pages/home.yml: every workflow step needs detail_title and detail");
                }
            }
            if (!stages.equals(steps)) {
                problems.add("pages/home.yml: hero.workflow.stages and workflow.steps must use the same steps " + stages + " / " + steps);
            }
            for (Object logo : (List<Object>) ((Map<String, Object>) home.get("brands")).get("logos")) {
                Map<String, Object> l = (Map<String, Object>) logo;
                if (blank(l.get("name")) || !String.valueOf(l.get("src")).startsWith("/assets/brands/")) {
                    problems.add("pages/home.yml: logo " + l.get("name") + " needs a name and an image in /assets/brands/");
                }
            }
        } catch (RuntimeException e) {
            problems.add("pages/home.yml: hero.workflow, workflow.steps and brands.logos must keep their structure");
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkForm(String file, Map<String, Object> page, List<String> problems) {
        try {
            Map<String, Object> form = (Map<String, Object>) page.get("form");
            Map<String, Object> hubspot = (Map<String, Object>) form.get("hubspot");
            if (!String.valueOf(hubspot.get("portal_id")).matches("\\d+")
                    || !String.valueOf(hubspot.get("form_id")).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                    || !String.valueOf(hubspot.get("region")).matches("[a-z0-9]+")) {
                problems.add(file + ": form.hubspot needs a numeric portal_id, a HubSpot form_id and a region");
            }
            if (!String.valueOf(form.get("key")).matches("[a-z][a-z0-9-]*")) {
                problems.add(file + ": form.key must be lower-case letters, digits or dashes");
            }
            Map<String, Object> fallback = (Map<String, Object>) form.get("fallback");
            if (!Html.isAllowedUrl("mailto:" + fallback.get("recipient"))) {
                problems.add(file + ": form.fallback.recipient must be an email address");
            }
            for (Object field : (List<Object>) fallback.get("fields")) {
                Map<String, Object> f = (Map<String, Object>) field;
                if (!Set.of("email", "tel", "text", "textarea").contains(String.valueOf(f.get("type"))) || blank(f.get("name")) || blank(f.get("label"))) {
                    problems.add(file + ": every form field needs a label, a name and a type (email, tel, text or textarea)");
                }
            }
        } catch (RuntimeException e) {
            problems.add(file + ": form must keep its structure (key, hubspot, fallback.fields)");
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkGuide(Map<String, Object> guide, List<String> problems) {
        try {
            Map<String, Object> messages = (Map<String, Object>) guide.get("messages");
            for (String key : GUIDE_MESSAGES) {
                if (blank(messages.get(key))) {
                    problems.add("guide.yml: messages." + key + " is required");
                }
            }
            Set<String> ids = new HashSet<>();
            for (Object topic : (List<Object>) guide.get("topics")) {
                Map<String, Object> t = (Map<String, Object>) topic;
                if (!ids.add(String.valueOf(t.get("id")))) {
                    problems.add("guide.yml: topic id " + t.get("id") + " is used twice");
                }
                if (blank(t.get("label")) || blank(t.get("text")) || !(t.get("terms") instanceof List<?> terms) || terms.isEmpty()) {
                    problems.add("guide.yml: topic " + t.get("id") + " needs a label, matching terms and text");
                }
                if (!String.valueOf(t.get("url")).startsWith("/")) {
                    problems.add("guide.yml: topic " + t.get("id") + " must link to a SecurePay page path");
                }
            }
        } catch (RuntimeException e) {
            problems.add("guide.yml: panel, messages and topics must keep their structure");
        }
    }

    private static void require(Map<String, Object> map, String file, List<String> problems, String... keys) {
        for (String key : keys) {
            if (map.get(key) == null) {
                problems.add(file + ": " + key + " is required");
            }
        }
    }

    private static boolean blank(Object value) {
        return value == null || value.toString().isBlank();
    }
}
