package ae.securepay.website.guide;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 20 Guide questions per visitor address in a fixed 10-minute window; later questions in the window do not extend it.
 *
 * <p>The address is {@code request.getRemoteAddr()}. Behind a load balancer or CDN, configure Tomcat's
 * {@code RemoteIpValve} with the proxies' addresses (see README) so this is the visitor's address, not the proxy's.
 * Limits are per Tomcat instance.
 */
@Component
public class RateLimiter {

    static final int LIMIT = 20;
    static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int MAX_TRACKED = 50_000;

    private record Window(int count, long until) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Records a question and returns true when the visitor is over the limit. */
    public boolean limited(String visitor) {
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED) {
            windows.values().removeIf(window -> window.until() <= now);
        }
        boolean[] limited = {false};
        windows.compute(visitor, (key, window) -> {
            if (window == null || window.until() <= now) {
                return new Window(1, now + WINDOW.toMillis());
            }
            if (window.count() >= LIMIT) {
                limited[0] = true;
                return window;
            }
            return new Window(window.count() + 1, window.until());
        });
        return limited[0];
    }
}
