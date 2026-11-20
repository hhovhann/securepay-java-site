package ae.securepay.website;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * JSP prints ${...} without escaping. Every value shown on a page must therefore go through an sp: function
 * (which escapes it). Expressions are allowed bare only where they are not printed: JSTL test/items/value attributes,
 * or a choice between two fixed words.
 */
class JspSafetyTest {

    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{([^}]*)}");
    private static final Pattern NOT_PRINTED = Pattern.compile("(?:test|items|value)=\"$");
    private static final Pattern FIXED_CHOICE = Pattern.compile("[\\w.]+ \\? '[\\w-]*' : '[\\w-]*'");

    @Test
    void everyPrintedValueIsEscaped() throws IOException {
        List<String> unsafe = new ArrayList<>();
        try (Stream<Path> files = Files.walk(TestContent.WEBAPP.resolve("WEB-INF/jsp"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".jsp") || f.toString().endsWith(".jspf")).toList()) {
                String text = Files.readString(file);
                Matcher m = EXPRESSION.matcher(text);
                while (m.find()) {
                    String inner = m.group(1).trim();
                    String before = text.substring(Math.max(0, m.start() - 8), m.start());
                    if (inner.startsWith("sp:") || NOT_PRINTED.matcher(before).find() || FIXED_CHOICE.matcher(inner).matches()) {
                        continue;
                    }
                    long line = text.substring(0, m.start()).chars().filter(c -> c == '\n').count() + 1;
                    unsafe.add(TestContent.WEBAPP.relativize(file) + ":" + line + " ${" + inner + "}");
                }
            }
        }
        assertTrue(unsafe.isEmpty(), "Unescaped values: " + unsafe);
    }
}
