package com.clinic.platform.notification.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Section 53's example: "Your appointment with {{doctorName}} is confirmed for {{date}} at
 * {{time}}." Deliberately minimal — no conditionals, no loops, just variable substitution.
 * A missing variable renders as an empty string rather than throwing, since a malformed
 * template should degrade to a slightly-odd message, not block the whole notification.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    public String render(String template, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = variables.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
