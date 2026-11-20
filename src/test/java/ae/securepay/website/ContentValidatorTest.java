package ae.securepay.website;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ae.securepay.website.content.ContentStore;
import ae.securepay.website.content.ContentValidator;
import ae.securepay.website.content.SitePage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** The published content passes every rule, and the rules catch the mistakes an edit could introduce. */
class ContentValidatorTest {

    @Test
    void publishedContentIsValid() {
        ContentStore store = TestContent.store();
        assertTrue(store.page(SitePage.HOME).containsKey("hero"));
    }

    @Test
    void htmlInContentIsRejected() throws IOException {
        List<String> problems = validateWith(SitePage.ABOUT_US, page -> section(page, "intro").put("text", "Hello <script>alert(1)</script>"));
        assertTrue(problems.stream().anyMatch(p -> p.contains("intro.text contains HTML")), problems::toString);
    }

    @Test
    void brokenLinksAreRejected() throws IOException {
        List<String> problems = validateWith(SitePage.ABOUT_US, page -> section(page, "cta").put("url", "/contact-page/"));
        assertTrue(problems.stream().anyMatch(p -> p.contains("page that does not exist: /contact-page/")), problems::toString);
        problems = validateWith(SitePage.ABOUT_US, page -> section(page, "intro").put("text", "See [this](javascript:alert(1))"));
        assertTrue(problems.stream().anyMatch(p -> p.contains("not allowed")), problems::toString);
    }

    @Test
    void missingSectionsAndFilesAreRejected() throws IOException {
        List<String> problems = validateWith(SitePage.SERVICES, page -> section(page, "intro").put("tabs",
                List.of(Map.of("label", "Payment Links", "url", "#payment-link"))));
        assertTrue(problems.stream().anyMatch(p -> p.contains("section that does not exist on this page: #payment-link")), problems::toString);
        problems = validateWith(SitePage.ABOUT_US, page -> section(page, "image").put("src", "/assets/img/pages/missing.png"));
        assertTrue(problems.stream().anyMatch(p -> p.contains("file that is not in the website")), problems::toString);
    }

    @Test
    void searchSummaryIsRequired() throws IOException {
        List<String> problems = validateWith(SitePage.PARTNERS, page -> section(page, "seo").remove("description"));
        assertTrue(problems.stream().anyMatch(p -> p.contains("seo.title and seo.description are required")), problems::toString);
    }

    @Test
    void hubspotIdsMustStayValid() throws IOException {
        List<String> problems = validateWith(SitePage.CONTACT_US, page -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> form = (Map<String, Object>) page.get("form");
            @SuppressWarnings("unchecked")
            Map<String, Object> hubspot = (Map<String, Object>) form.get("hubspot");
            hubspot.put("form_id", "not-a-form");
        });
        assertTrue(problems.stream().anyMatch(p -> p.contains("form.hubspot")), problems::toString);
    }

    interface Change {
        void apply(Map<String, Object> page);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> page, String key) {
        return (Map<String, Object>) page.get(key);
    }

    private static List<String> validateWith(SitePage changed, Change change) throws IOException {
        Map<SitePage, Map<String, Object>> pages = new EnumMap<>(SitePage.class);
        for (SitePage page : SitePage.values()) {
            pages.put(page, read("pages/" + page.slug() + ".yml"));
        }
        change.apply(pages.get(changed));
        return ContentValidator.validate(read("site.yml"), pages, read("guide.yml"), TestContent::assetExists);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(String name) throws IOException {
        try (InputStream in = Files.newInputStream(TestContent.CONTENT.resolve(name))) {
            return (Map<String, Object>) new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        }
    }
}
