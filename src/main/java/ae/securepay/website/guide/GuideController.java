package ae.securepay.website.guide;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/guide/ask {"question": "..."} → {"answer", "sources", "mode"} or {"error"}. */
@RestController
public class GuideController {

    private final GuideService guide;
    private final RateLimiter rateLimiter;

    public GuideController(GuideService guide, RateLimiter rateLimiter) {
        this.guide = guide;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping(value = "/api/guide/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ask(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Object raw = body == null ? null : body.get("question");
        String question = raw instanceof String text ? text.strip() : "";
        if (question.isEmpty() || question.codePointCount(0, question.length()) > 500) {
            return error(HttpStatus.BAD_REQUEST, "invalid");
        }
        if (GuideRules.hasSensitiveData(question)) {
            return reply(guide.message("sensitive"), List.of(), "refused");
        }
        if (GuideRules.isPromptAttack(question)) {
            return reply(guide.message("out_of_scope"), List.of(), "refused");
        }
        if (!sameOrigin(request)) {
            return error(HttpStatus.FORBIDDEN, "origin_blocked");
        }
        if (rateLimiter.limited(request.getRemoteAddr())) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "rate_limited");
        }
        GuideService.Answer answer = guide.answer(question);
        List<Map<String, String>> sources = answer.sources().stream()
                .map(source -> Map.of("label", source.label(), "url", request.getContextPath() + source.url()))
                .toList();
        return reply(answer.text(), sources, answer.mode());
    }

    /** Browsers send Origin on cross-site POSTs; only this site's own pages may use the Guide. */
    private static boolean sameOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return true;
        }
        try {
            String host = URI.create(origin).getHost();
            return host != null && host.toLowerCase(Locale.ROOT).equals(request.getServerName().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private ResponseEntity<Map<String, Object>> reply(String answer, List<?> sources, String mode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("answer", answer);
        body.put("sources", sources);
        body.put("mode", mode);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(body);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String messageKey) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(Map.of("error", guide.message(messageKey)));
    }
}
