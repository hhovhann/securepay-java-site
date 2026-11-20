package ae.securepay.website;

import ae.securepay.website.content.ContentStore;
import java.nio.file.Files;
import java.nio.file.Path;

/** The real content/ folder and web assets, as the tests see them. */
public final class TestContent {

    public static final Path CONTENT = Path.of("content").toAbsolutePath();
    public static final Path WEBAPP = Path.of("src/main/webapp").toAbsolutePath();

    private TestContent() {
    }

    public static boolean assetExists(String path) {
        return Files.isRegularFile(WEBAPP.resolve(path.substring(1)));
    }

    public static ContentStore store() {
        return new ContentStore(CONTENT, TestContent::assetExists);
    }
}
