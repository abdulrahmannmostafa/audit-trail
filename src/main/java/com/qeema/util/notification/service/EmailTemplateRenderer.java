package com.qeema.util.notification.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.qeema.util.notification.utility.TemplateRenderUtility;

@Service
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final TemplateRenderUtility renderUtility;

    public String renderSubject(String template, Map<String, Object> variables) {
        return renderUtility.render(template, variables, false);
    }

    public String renderHtml(String template, Map<String, Object> variables) {
        return renderUtility.render(template, variables, true);
    }
}
