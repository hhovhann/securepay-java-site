package ae.securepay.website.guide;

import java.util.List;
import java.util.regex.Pattern;

/**
 * What the SecurePay Guide refuses. The browser applies the same rules before sending (site.js), so sensitive data
 * normally never leaves the visitor's device; the server checks again because requests can bypass the browser.
 */
public final class GuideRules {

    private static final String LABEL = "(?:credit card number|card number|card details|cvv|cvc|security code|booking reference|"
            + "booking ref|reservation number|confirmation number|account number|password|passcode|api key|secret key|access token)s?";

    /** Card numbers, or a sensitive label followed by a value ("CVV 123", "password: …"), or a long key-like token. */
    private static final List<Pattern> SENSITIVE = List.of(
            Pattern.compile("(?:^|\\D)(?:\\d[ -]?){13,19}(?!\\d)"),
            Pattern.compile("\\b" + LABEL + "\\b\\W{0,4}(?:(?:is|was)\\W{1,3})?[a-z0-9_-]*\\d[a-z0-9_-]*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b" + LABEL + "\\s*[:=]\\s*\\S{4,}", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?=[a-z0-9_-]*\\d)(?=[a-z0-9_-]*[a-z])[a-z0-9_-]{28,}\\b", Pattern.CASE_INSENSITIVE));

    /** Attempts to change the Guide's role or extract its instructions. Product wording such as "act as our gateway" is allowed. */
    private static final Pattern PROMPT_ATTACK = Pattern.compile(
            "\\b(?:ignore|disregard|override|forget)\\s+(?:(?:all|any|the|these|those)\\s+)*(?:instructions|prompts?|guidelines)\\b"
                    + "|\\b(?:ignore|disregard|override|forget)\\b.{0,25}\\b(?:previous|prior|above|earlier|your|system|developer)\\b.{0,15}\\b(?:instructions|rules|prompts?|guidelines|messages?)\\b"
                    + "|\\b(?:system prompt|hidden prompt|developer message|initial instructions|jailbreak|roleplay as|pretend (?:to be|you are)|you are now (?:a|an|my)|dan mode)\\b"
                    + "|\\b(?:reveal|repeat|print|show|display|leak|output)\\b.{0,25}\\byour\\s+(?:instructions|prompt|rules|api key|secrets?|credentials|configuration)\\b",
            Pattern.CASE_INSENSITIVE);

    private GuideRules() {
    }

    public static boolean hasSensitiveData(String question) {
        return SENSITIVE.stream().anyMatch(pattern -> pattern.matcher(question).find());
    }

    public static boolean isPromptAttack(String question) {
        return PROMPT_ATTACK.matcher(question).find();
    }
}
