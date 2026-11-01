package com.knowledgebase.helpdesk.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds article-list URLs for filter chips and view toggles, omitting blank
 * parameters so links stay clean. URLs are computed in the controller because
 * Thymeleaf's restricted mode does not allow method calls inside th:href.
 */
final class ArticleLinks {

    private ArticleLinks() {
    }

    static String list(String keyword, String status, String sort, String view) {
        StringBuilder url = new StringBuilder("/articles");
        add(url, "keyword", keyword);
        add(url, "status", status);
        add(url, "sort", sort);
        add(url, "view", view);
        return url.toString();
    }

    private static void add(StringBuilder url, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        url.append(url.indexOf("?") < 0 ? '?' : '&')
                .append(name)
                .append('=')
                .append(URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
    }
}
