package ae.securepay.website.content;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns content text into safe HTML. Used from the JSP pages as the {@code sp:} functions.
 *
 * <p>Content is plain text with three formatting marks: a new line becomes a line break, {@code [label](url)} becomes
 * a link and {@code ==text==} is shown in the accent colour. Everything else is HTML-escaped, so content can never add
 * markup or scripts to a page.
 */
public final class Html {

    private static final Pattern LINK = Pattern.compile("\\[([^\\]\\n]+)\\]\\(([^)\\s]+)\\)");
    private static final Pattern ACCENT = Pattern.compile("==(.+?)==");
    private static final Pattern EMAIL = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final ThreadLocal<String> CONTEXT_PATH = ThreadLocal.withInitial(() -> "");

    private Html() {
    }

    /** Set per request by {@code SiteFilter} so internal links work when the WAR is not deployed at the site root. */
    public static void setContextPath(String contextPath) {
        CONTEXT_PATH.set(contextPath == null ? "" : contextPath);
    }

    public static void clearContextPath() {
        CONTEXT_PATH.remove();
    }

    /** Formatted text: escaped, with links, accents and line breaks. */
    public static String text(Object value) {
        String escaped = escape(value == null ? "" : value.toString());
        Matcher links = LINK.matcher(escaped);
        StringBuilder out = new StringBuilder();
        while (links.find()) {
            String target = unescape(links.group(2));
            String replacement = isAllowedUrl(target)
                    ? "<a href=\"" + escape(resolve(target)) + "\">" + links.group(1) + "</a>"
                    : links.group(0);
            links.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        links.appendTail(out);
        String accented = ACCENT.matcher(out).replaceAll("<span class=\"accent\">$1</span>");
        return accented.replace("\r\n", "\n").replace("\n", "<br>");
    }

    /** Text for attributes, titles and meta tags: formatting marks removed, then escaped. */
    public static String plain(Object value) {
        String text = value == null ? "" : value.toString();
        text = LINK.matcher(text).replaceAll("$1");
        text = ACCENT.matcher(text).replaceAll("$1");
        return escape(text.replaceAll("\\s*\\n\\s*", " ").trim());
    }

    /** Escaped value for an attribute. */
    public static String attr(Object value) {
        return escape(value == null ? "" : value.toString());
    }

    /** A safe link target, with the deployment path added to internal links. Unsafe or unknown schemes become "#". */
    public static String url(Object value) {
        String target = value == null ? "" : value.toString().trim();
        return isAllowedUrl(target) ? escape(resolve(target)) : "#";
    }

    /** Allowed link targets: site paths, in-page anchors, https:, mailto: and tel:. */
    public static boolean isAllowedUrl(String url) {
        // Browsers read a backslash as a slash, so "/\evil.example" would leave the site like "//evil.example".
        if (url == null || url.isBlank() || url.chars().anyMatch(Character::isWhitespace) || url.indexOf('\\') >= 0) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/") && !lower.startsWith("//") && !lower.contains("..")) {
            return true;
        }
        if (lower.startsWith("#") && lower.length() > 1) {
            return true;
        }
        if (lower.startsWith("https://") && lower.length() > "https://".length()) {
            return true;
        }
        if (lower.startsWith("mailto:")) {
            return EMAIL.matcher(lower.substring("mailto:".length())).matches();
        }
        return lower.startsWith("tel:") && lower.substring(4).matches("\\+?[0-9]{6,15}");
    }

    private static String resolve(String url) {
        return url.startsWith("/") ? CONTEXT_PATH.get() + url : url;
    }

    public static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String unescape(String text) {
        return text.replace("&#39;", "'").replace("&quot;", "\"").replace("&gt;", ">").replace("&lt;", "<").replace("&amp;", "&");
    }
}
