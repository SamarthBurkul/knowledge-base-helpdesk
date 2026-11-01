package com.knowledgebase.helpdesk.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleLinksTest {

    @Test
    void shouldOmitBlankParameters() {
        assertEquals("/articles", ArticleLinks.list(null, "", " ", null));
        assertEquals("/articles?status=DRAFT&view=table",
                ArticleLinks.list("", "DRAFT", null, "table"));
    }

    @Test
    void shouldEncodeKeyword() {
        assertEquals("/articles?keyword=C%2B%2B+%7Bsetup%7D&sort=title",
                ArticleLinks.list("C++ {setup}", null, "title", null));
    }
}
