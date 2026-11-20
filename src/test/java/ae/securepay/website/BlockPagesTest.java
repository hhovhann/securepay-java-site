package ae.securepay.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ae.securepay.website.content.ContentStore;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.junit.jupiter.api.Test;

/** A new page can be added from content alone: one YAML file of blocks (plus a menu link), no code. */
class BlockPagesTest {

    /** Test-only page using every block type. */
    private static final String PAGE = """
            layout: "blocks"
            seo:
              title: "Block test page | SecurePay"
              description: "Test page built from blocks."
            blocks:
              - type: "intro"
                eyebrow: "TEST EYEBROW"
                title: "Block test\\n==heading=="
                text: "Intro text with a [link](/services/#smartsell)."
              - type: "image"
                src: "/assets/img/pages/about-us.png"
                alt: "Test image alt"
              - type: "facts"
                items:
                  - value: "Fact value"
                    label: "Fact label"
              - type: "cards"
                id: "test-cards"
                eyebrow: "CARDS EYEBROW"
                title: "Cards title"
                cards:
                  - label: "01"
                    title: "Card title"
                    text: "Card text"
                    points: ["Card point"]
              - type: "details"
                title: "Details title"
                text: "Details text"
                groups:
                  - title: "Group title"
                    text: "Group text"
                    points: ["Group point"]
              - type: "text"
                title: "Text title"
                paragraphs: ["Paragraph one", "Paragraph two & more"]
              - type: "cta"
                title: "CTA title"
                link_label: "CTA link"
                url: "/block-test/#test-cards"
            """;

    private static Path contentWith(String pageName, String page, boolean addMenuLink) throws IOException {
        Path dir = Files.createTempDirectory("securepay-content");
        try (Stream<Path> files = Files.walk(TestContent.CONTENT)) {
            for (Path source : files.toList()) {
                Path target = dir.resolve(TestContent.CONTENT.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
        Files.writeString(dir.resolve("pages/" + pageName + ".yml"), page);
        if (addMenuLink) {
            Path site = dir.resolve("site.yml");
            Files.writeString(site, Files.readString(site).replace("menu:\n", "menu:\n  - label: \"Block test\"\n    url: \"/block-test/\"\n"));
        }
        return dir;
    }

    @Test
    void aNewPageIsServedFromContentAlone() throws Exception {
        Path content = contentWith("block-test", PAGE, true);
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("securepay-tomcat").toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        Context context = tomcat.addWebapp("", TestContent.WEBAPP.toString());
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", Path.of("target/classes").toAbsolutePath().toString(), "/"));
        context.setResources(resources);
        System.setProperty("securepay.content.dir", content.toString());
        try {
            tomcat.start();
        } finally {
            System.clearProperty("securepay.content.dir");
        }
        try {
            String base = "http://localhost:" + tomcat.getConnector().getLocalPort();
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            HttpResponse<String> page = http.send(HttpRequest.newBuilder(URI.create(base + "/block-test/")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
            String html = page.body();
            for (String expected : new String[] {"<title>Block test page | SecurePay</title>", "Block test<br><span class=\"accent\">heading</span>",
                    "<a href=\"/services/#smartsell\">link</a>", "alt=\"Test image alt\"", "Fact value", "id=\"test-cards\"", "Card point",
                    "Group point", "Paragraph two &amp; more", "href=\"/block-test/#test-cards\">CTA link</a>",
                    "<a class=\"nav-link\" href=\"/block-test/\"><span class=\"nav-label\">Block test</span>"}) {
                assertTrue(html.contains(expected), "missing: " + expected);
            }
            assertTrue(http.send(HttpRequest.newBuilder(URI.create(base + "/sitemap.xml")).build(), HttpResponse.BodyHandlers.ofString())
                    .body().contains("<loc>https://public.securepay.ae/block-test/</loc>"));
            assertEquals(301, http.send(HttpRequest.newBuilder(URI.create(base + "/block-test")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @Test
    void mistakesInANewPageAreExplained() throws IOException {
        ContentStore.ContentException wrongType = assertThrows(ContentStore.ContentException.class, () ->
                new ContentStore(contentWith("block-test", PAGE.replace("type: \"facts\"", "type: \"gallery\""), false), TestContent::assetExists));
        assertTrue(wrongType.getMessage().contains("blocks[3].type \"gallery\" is not available"), wrongType.getMessage());

        ContentStore.ContentException reserved = assertThrows(ContentStore.ContentException.class, () ->
                new ContentStore(contentWith("api", PAGE, false), TestContent::assetExists));
        assertTrue(reserved.getMessage().contains("pages/api.yml: the file name becomes the page address"), reserved.getMessage());

        ContentStore.ContentException noIntro = assertThrows(ContentStore.ContentException.class, () ->
                new ContentStore(contentWith("block-test", PAGE.replace("type: \"intro\"", "type: \"text\"\n    paragraphs: [\"x\"]"), false), TestContent::assetExists));
        assertTrue(noIntro.getMessage().contains("the first block must be an intro"), noIntro.getMessage());

        ContentStore.ContentException brokenLink = assertThrows(ContentStore.ContentException.class, () ->
                new ContentStore(contentWith("block-test", PAGE.replace("/block-test/#test-cards", "/block-test/#missing"), false), TestContent::assetExists));
        assertTrue(brokenLink.getMessage().contains("links to a section that does not exist: /block-test/#missing"), brokenLink.getMessage());
    }
}
