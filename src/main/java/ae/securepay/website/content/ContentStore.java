package ae.securepay.website.content;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loads the website content (content/*.yml) and checks it before it is shown.
 *
 * <p>In production the content is packaged inside the WAR and loaded once; invalid content stops the deployment
 * instead of publishing a broken page. For previews, set {@code SECUREPAY_CONTENT_DIR} (or the system property
 * {@code securepay.content.dir}) to a content folder: edits are picked up on the next page load, and an invalid edit
 * keeps the last valid version on screen while the problem is logged.
 */
@Component
public class ContentStore {

    private static final System.Logger LOG = System.getLogger(ContentStore.class.getName());

    /** Content as loaded: the seven core pages, any new block pages (by address name), the site and the Guide. */
    public record Snapshot(Map<String, Object> site, Map<SitePage, Map<String, Object>> pages,
                           Map<String, Map<String, Object>> blockPages, Map<String, Object> guide) {
    }

    private final Path directory;
    private final Predicate<String> assetExists;
    private final Supplier<List<String>> packagedPageFiles;
    private volatile Snapshot current;
    private volatile long loadedStamp;

    @Autowired
    public ContentStore(ServletContext servletContext) {
        this(configuredDirectory(), path -> {
            try {
                return servletContext.getResource(path) != null;
            } catch (MalformedURLException e) {
                return false;
            }
        }, () -> {
            Set<String> paths = servletContext.getResourcePaths("/WEB-INF/classes/content/pages/");
            return paths == null ? List.of() : paths.stream().map(path -> path.substring(path.lastIndexOf('/') + 1)).toList();
        });
    }

    public ContentStore(Path directory, Predicate<String> assetExists) {
        this(directory, assetExists, List::of);
    }

    private ContentStore(Path directory, Predicate<String> assetExists, Supplier<List<String>> packagedPageFiles) {
        this.directory = directory;
        this.assetExists = assetExists;
        this.packagedPageFiles = packagedPageFiles;
        this.loadedStamp = lastModified();
        this.current = load();
    }

    public Map<String, Object> site() {
        return snapshot().site();
    }

    public Map<String, Object> page(SitePage page) {
        return snapshot().pages().get(page);
    }

    public Map<String, Object> guide() {
        return snapshot().guide();
    }

    /** A page built from blocks (content/pages/&lt;name&gt;.yml with layout: blocks), or null. */
    public Map<String, Object> blockPage(String name) {
        return snapshot().blockPages().get(name);
    }

    /** Names of the block pages, in address order. */
    public List<String> blockPageNames() {
        return List.copyOf(snapshot().blockPages().keySet());
    }

    private Snapshot snapshot() {
        if (directory != null) {
            long stamp = lastModified();
            if (stamp != loadedStamp) {
                loadedStamp = stamp;
                try {
                    current = load();
                    LOG.log(System.Logger.Level.INFO, "Reloaded website content from " + directory);
                } catch (ContentException e) {
                    LOG.log(System.Logger.Level.WARNING, "Content not reloaded: " + e.getMessage());
                }
            }
        }
        return current;
    }

    private Snapshot load() {
        Map<String, Object> site = read("site.yml");
        Map<String, Object> guide = read("guide.yml");
        Map<SitePage, Map<String, Object>> pages = new EnumMap<>(SitePage.class);
        for (SitePage page : SitePage.values()) {
            pages.put(page, read("pages/" + page.slug() + ".yml"));
        }
        Map<String, Map<String, Object>> blockPages = new TreeMap<>();
        for (String file : pageFiles()) {
            String name = file.substring(0, file.length() - ".yml".length());
            if (SitePage.bySlug(name).isEmpty()) {
                blockPages.put(name, read("pages/" + file));
            }
        }
        List<String> problems = ContentValidator.validate(site, pages, blockPages, guide, assetExists);
        if (!problems.isEmpty()) {
            throw new ContentException("Website content has " + problems.size() + " problem(s):\n - " + String.join("\n - ", problems));
        }
        return new Snapshot(site, pages, blockPages, guide);
    }

    private Map<String, Object> read(String name) {
        try (InputStream in = open(name)) {
            if (in == null) {
                throw new ContentException("Missing content file content/" + name);
            }
            Object data = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
            if (!(data instanceof Map<?, ?> map)) {
                throw new ContentException("content/" + name + " must contain named fields");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (RuntimeException e) {
            if (e instanceof ContentException) {
                throw e;
            }
            throw new ContentException("content/" + name + " could not be read: " + e.getMessage());
        }
    }

    private List<String> pageFiles() {
        List<String> files;
        if (directory != null) {
            try (Stream<Path> list = Files.list(directory.resolve("pages"))) {
                files = list.map(path -> path.getFileName().toString()).toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            files = packagedPageFiles.get();
        }
        return files.stream().filter(name -> name.endsWith(".yml")).sorted().toList();
    }

    private InputStream open(String name) throws IOException {
        if (directory != null) {
            Path file = directory.resolve(name);
            return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
        }
        return ContentStore.class.getClassLoader().getResourceAsStream("content/" + name);
    }

    private long lastModified() {
        if (directory == null) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(file -> {
                try {
                    return Files.getLastModifiedTime(file).toMillis() + file.toString().length();
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            return -1;
        }
    }

    private static Path configuredDirectory() {
        String value = System.getProperty("securepay.content.dir", System.getenv("SECUREPAY_CONTENT_DIR"));
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    /** Content that cannot be published. */
    public static class ContentException extends RuntimeException {
        public ContentException(String message) {
            super(message);
        }
    }
}
