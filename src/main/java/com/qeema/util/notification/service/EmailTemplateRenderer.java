package com.qeema.util.notification.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateRenderer {

    // Regular expression pattern to match placeholders in the format
    // {{variableName}}
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    public String renderSubject(String template, Map<String, Object> variables) {
        return render(template, variables, false);
    }

    public String renderHtml(String template, Map<String, Object> variables) {
        return render(template, variables, true);
    }

    private String render(
            String template,
            Map<String, Object> variables,
            boolean escapeHtml) {
        if (template == null) {
            throw new IllegalArgumentException("Template content must not be null");
        }

        Map<String, Object> safeVariables = variables == null ? Map.of() : variables;

        // Use a StringBuffer to build the result while replacing placeholders
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = safeVariables.get(name);
            if (value == null) {
                throw new IllegalArgumentException(
                        "Missing template variable: " + name);
            }

            String replacement = String.valueOf(value);
            if (escapeHtml) {
                replacement = escapeHtml(replacement);
            }
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
