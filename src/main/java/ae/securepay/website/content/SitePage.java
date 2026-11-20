package ae.securepay.website.content;

import java.util.Arrays;
import java.util.Optional;

/** The site's pages. Paths are the same as on the current WordPress site, so existing links and search results keep working. */
public enum SitePage {
    HOME("home", "/"),
    ABOUT_US("about-us", "/about-us/"),
    SERVICES("services", "/services/"),
    CONTACT_US("contact-us", "/contact-us/"),
    PARTNERS("partners", "/partners/"),
    PRIVACY_POLICY("privacy-policy", "/privacy-policy/"),
    GENERAL_TERMS_CONDITIONS("general-terms-conditions", "/general-terms-conditions/");

    private final String slug;
    private final String path;

    SitePage(String slug, String path) {
        this.slug = slug;
        this.path = path;
    }

    public String slug() {
        return slug;
    }

    public String path() {
        return path;
    }

    public static Optional<SitePage> bySlug(String slug) {
        return Arrays.stream(values()).filter(page -> page.slug.equals(slug)).findFirst();
    }

    public static Optional<SitePage> byPath(String path) {
        return Arrays.stream(values()).filter(page -> page.path.equals(path)).findFirst();
    }
}
