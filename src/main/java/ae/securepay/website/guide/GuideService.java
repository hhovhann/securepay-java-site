package ae.securepay.website.guide;

import ae.securepay.website.content.ContentStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Answers visitor questions from the approved public facts in content/guide.yml. No AI model is connected in this
 * pilot: the answer is the matched published text, with a link to the page it comes from.
 */
@Service
public class GuideService {

    public record Source(String label, String url) {
    }

    public record Answer(String text, List<Source> sources, String mode) {
    }

    private final ContentStore content;

    public GuideService(ContentStore content) {
        this.content = content;
    }

    public String message(String key) {
        return String.valueOf(messages().get(key));
    }

    /** The best one or two topics, or a "no verified answer" reply. */
    public Answer answer(String question) {
        List<Map<String, Object>> matches = match(question);
        if (matches.isEmpty()) {
            return new Answer(message("no_answer"), List.of(), "no-match");
        }
        List<String> texts = new ArrayList<>();
        List<Source> sources = new ArrayList<>();
        for (Map<String, Object> topic : matches) {
            texts.add(String.valueOf(topic.get("text")));
            sources.add(new Source(String.valueOf(topic.get("label")), String.valueOf(topic.get("url"))));
        }
        return new Answer(String.join("\n\n", texts), sources, "grounded");
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> match(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        List<Map.Entry<Map<String, Object>, Integer>> ranked = new ArrayList<>();
        for (Object item : (List<Object>) content.guide().get("topics")) {
            Map<String, Object> topic = (Map<String, Object>) item;
            int score = 0;
            for (Object term : (List<Object>) topic.get("terms")) {
                String t = String.valueOf(term).toLowerCase(Locale.ROOT).trim();
                if (!t.isEmpty() && termMatches(normalized, t)) {
                    score += (t.contains(" ") || t.contains("-") || t.contains("&")) ? 3 : 1;
                }
            }
            if (score > 0) {
                ranked.add(Map.entry(topic, score));
            }
        }
        ranked.sort(Comparator.comparing((Map.Entry<Map<String, Object>, Integer> e) -> e.getValue()).reversed());
        return ranked.stream().limit(2).map(Map.Entry::getKey).toList();
    }

    /**
     * Whole-word match: a term never matches inside another word ('pos' in 'deposit', 'spa' in 'Spain', 'api' in
     * 'capital'). Terms of five or more characters also match longer forms of the word (group → groups).
     */
    public static boolean termMatches(String text, String term) {
        String pattern = "(?<![a-z0-9])" + Pattern.quote(term) + (term.length() <= 4 ? "(?:s|es)?(?![a-z0-9])" : "");
        return Pattern.compile(pattern).matcher(text).find();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> messages() {
        Object messages = content.guide().get("messages");
        return messages instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }
}
