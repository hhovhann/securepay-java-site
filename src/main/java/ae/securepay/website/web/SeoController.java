package ae.securepay.website.web;

import ae.securepay.website.content.ContentStore;
import ae.securepay.website.content.Html;
import ae.securepay.website.content.SitePage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** robots.txt and sitemap.xml, which the WordPress SEO plugin provides today. */
@RestController
public class SeoController {

    private final ContentStore content;

    public SeoController(ContentStore content) {
        this.content = content;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return "User-agent: *\nAllow: /\n\nSitemap: " + baseUrl() + "/sitemap.xml\n";
    }

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (SitePage page : SitePage.values()) {
            xml.append("  <url><loc>").append(Html.escape(baseUrl() + page.path())).append("</loc></url>\n");
        }
        for (String name : content.blockPageNames()) {
            xml.append("  <url><loc>").append(Html.escape(baseUrl() + "/" + name + "/")).append("</loc></url>\n");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }

    private String baseUrl() {
        return String.valueOf(content.site().get("base_url")).replaceAll("/+$", "");
    }
}
