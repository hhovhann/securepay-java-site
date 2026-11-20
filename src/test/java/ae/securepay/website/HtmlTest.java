package ae.securepay.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ae.securepay.website.content.Html;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HtmlTest {

    @AfterEach
    void reset() {
        Html.clearContextPath();
    }

    @Test
    void contentCannotAddMarkupOrScripts() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;x&quot; &#39;y&#39;", Html.text("<script>alert(1)</script> & \"x\" 'y'"));
        assertEquals("&lt;img src=x onerror=alert(1)&gt;", Html.plain("<img src=x onerror=alert(1)>"));
    }

    @Test
    void formattingMarks() {
        assertEquals("Get paid.<br><span class=\"accent\">Smarter.</span>", Html.text("Get paid.\n==Smarter.=="));
        assertEquals("Email <a href=\"mailto:sales@securepay.ae\">sales@securepay.ae</a>.", Html.text("Email [sales@securepay.ae](mailto:sales@securepay.ae)."));
        assertEquals("F&amp;B <a href=\"/services/#smartsell\">SmartSell</a>", Html.text("F&B [SmartSell](/services/#smartsell)"));
        assertEquals("Get paid. Smarter.", Html.plain("Get paid.\n==Smarter.=="));
        assertEquals("Email sales@securepay.ae", Html.plain("Email [sales@securepay.ae](mailto:sales@securepay.ae)"));
    }

    @Test
    void unsafeLinksAreNotLinked() {
        assertEquals("[click](javascript:alert(1))", Html.text("[click](javascript:alert(1))").replace("&#39;", "'"));
        assertEquals("#", Html.url("javascript:alert(1)"));
        assertEquals("#", Html.url("//evil.example/"));
        assertEquals("#", Html.url("/\\evil.example/"));
        assertEquals("#", Html.url("https://securepay.ae\\@evil.example/"));
        assertEquals("#", Html.url("data:text/html,hi"));
        assertEquals("#", Html.url("/../etc/passwd"));
        assertEquals("#", Html.url("http://insecure.example/"));
        assertFalse(Html.isAllowedUrl("mailto:not-an-email"));
        assertTrue(Html.isAllowedUrl("tel:+971504780251"));
        assertTrue(Html.isAllowedUrl("https://www.linkedin.com/company/securepay.ae/"));
    }

    @Test
    void internalLinksFollowTheDeploymentPath() {
        Html.setContextPath("/site");
        assertEquals("/site/about-us/", Html.url("/about-us/"));
        assertEquals("<a href=\"/site/contact-us/\">Contact</a>", Html.text("[Contact](/contact-us/)"));
        assertEquals("https://example.com/", Html.url("https://example.com/"));
        assertEquals("#payment-links", Html.url("#payment-links"));
    }
}
