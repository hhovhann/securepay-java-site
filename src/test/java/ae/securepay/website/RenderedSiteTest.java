package ae.securepay.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ae.securepay.website.content.Html;
import ae.securepay.website.content.SitePage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Deploys the web application into a real Tomcat 11 (the production server version) and checks the rendered pages:
 * every piece of content in content/*.yml must appear on its page, plus addresses, redirects, 404s, headers and assets.
 * The same application is also deployed under /site to prove links work when it is not at the site root.
 */
class RenderedSiteTest {

    private static Tomcat tomcat;
    private static String base;
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    /** Fields that are settings rather than visible text. */
    private static final Set<String> NOT_TEXT = Set.of("url", "src", "file", "step", "key", "portal_id", "form_id", "region",
            "type", "name", "required", "wide", "id", "logo", "social_image", "icon_32", "icon_192", "apple_touch_icon",
            "linkedin_url", "base_url", "recipient", "terms", "subject");

    @BeforeAll
    static void start() throws Exception {
        tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("securepay-tomcat").toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        deploy("");
        deploy("/site");
        tomcat.start();
        base = "http://localhost:" + tomcat.getConnector().getLocalPort();
    }

    private static void deploy(String contextPath) {
        Context context = tomcat.addWebapp(contextPath, TestContent.WEBAPP.toString());
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", Path.of("target/classes").toAbsolutePath().toString(), "/"));
        context.setResources(resources);
    }

    @AfterAll
    static void stop() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void everyPageShowsAllOfItsContent() throws Exception {
        Map<String, Object> site = read("site.yml");
        Map<String, Object> guide = read("guide.yml");
        List<String> missing = new ArrayList<>();
        for (SitePage page : SitePage.values()) {
            HttpResponse<String> response = get(page.path());
            assertEquals(200, response.statusCode(), page.path());
            String html = response.body();
            Map<String, Object> content = read("pages/" + page.slug() + ".yml");
            collect(content, null, html, page.path(), missing);
            collect(Map.of("menu", site.get("menu"), "footer", site.get("footer")), "site", html, page.path(), missing);
            collect(Map.of("panel", guide.get("panel")), "guide", html, page.path(), missing);
            assertTrue(html.contains("<title>" + Html.plain(((Map<?, ?>) content.get("seo")).get("title")) + "</title>"), page.path());
            assertTrue(html.contains("<link rel=\"canonical\" href=\"https://public.securepay.ae" + page.path() + "\">"), page.path());
        }
        assertTrue(missing.isEmpty(), "Content not shown: " + missing);
    }

    private static void collect(Object value, String key, String html, String path, List<String> missing) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (!NOT_TEXT.contains(String.valueOf(k)) && !"seo".equals(k)) {
                    collect(v, key == null ? String.valueOf(k) : key + "." + k, html, path, missing);
                }
            });
        } else if (value instanceof List<?> list) {
            list.forEach(item -> collect(item, key, html, path, missing));
        } else if (value instanceof String text) {
            if (!html.contains(Html.text(text)) && !html.contains(Html.plain(text)) && !html.contains(Html.attr(text))) {
                missing.add(path + " " + key + ": " + text);
            }
        }
    }

    @Test
    void addressesMatchTheCurrentSite() throws Exception {
        HttpResponse<String> redirect = get("/about-us");
        assertEquals(301, redirect.statusCode());
        assertEquals("/about-us/", URI.create(redirect.headers().firstValue("Location").orElseThrow()).getPath());
        HttpResponse<String> missing = get("/no-such-page/");
        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("Nothing Found"));
        assertTrue(missing.body().contains("<title>Page not found – SecurePay</title>"));
        assertEquals(404, get("/assets/css/missing.css").statusCode());
    }

    @Test
    void securityHeadersAndNoSessions() throws Exception {
        HttpResponse<String> home = get("/");
        assertEquals("nosniff", home.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("SAMEORIGIN", home.headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("strict-origin-when-cross-origin", home.headers().firstValue("Referrer-Policy").orElse(""));
        assertTrue(home.headers().allValues("Set-Cookie").isEmpty(), "the public site sets no cookies");
    }

    @Test
    void assetsSeoFilesAndGuideApi() throws Exception {
        for (String asset : List.of("/assets/css/base.css", "/assets/css/theme.css", "/assets/css/pages.css", "/assets/js/site.js",
                "/assets/img/securepay-logo.png", "/assets/brands/marriott.png", "/assets/legal/SecurePay-Terms-Conditions-Apr-2021-Dubai-Courts.pdf")) {
            assertEquals(200, get(asset).statusCode(), asset);
        }
        assertTrue(get("/robots.txt").body().contains("Sitemap: https://public.securepay.ae/sitemap.xml"));
        String sitemap = get("/sitemap.xml").body();
        for (SitePage page : SitePage.values()) {
            assertTrue(sitemap.contains("<loc>https://public.securepay.ae" + page.path() + "</loc>"), page.path());
        }
        HttpResponse<String> answer = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/guide/ask"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"What is SmartSell used for?\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, answer.statusCode(), answer.body());
        assertTrue(answer.body().contains("\"label\":\"SmartSell Promotions\""), answer.body());
    }

    @Test
    void linksWorkWhenDeployedUnderAPath() throws Exception {
        HttpResponse<String> page = get("/site/about-us/");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("href=\"/site/services/\""));
        assertTrue(page.body().contains("href=\"/site/assets/css/theme.css\""));
        assertTrue(page.body().contains("data-endpoint=\"/site/api/guide/ask\""));
        assertEquals(200, get("/site/assets/img/pages/about-us.png").statusCode());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> read(String name) throws IOException {
        try (InputStream in = Files.newInputStream(TestContent.CONTENT.resolve(name))) {
            return (Map<String, Object>) new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
        }
    }
}
