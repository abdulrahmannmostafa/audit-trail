package com.qeema.util.notification.utility;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TemplateRenderUtility {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    public String render(String template, Map<String, Object> variables, boolean escapeHtml) {
        if (template == null) {
            throw new IllegalArgumentException("Template content must not be null");
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();
        Map<String, Object> safeVariables = variables == null ? Map.of() : variables;

        while (matcher.find()) {
            Object value = safeVariables.get(matcher.group(1));
            if (value == null) {
                throw new IllegalArgumentException("Missing template variable: " + matcher.group(1));
            }
            String replacement = String.valueOf(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(escapeHtml
                    ? escapeHtml(replacement)
                    : replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
