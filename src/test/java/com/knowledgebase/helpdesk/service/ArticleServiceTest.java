package com.knowledgebase.helpdesk.service;

import com.knowledgebase.helpdesk.model.Article;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArticleServiceTest {

    @Test
    void shouldFindExistingArticles() {
        ArticleService service = new ArticleService();

        assertTrue(service.count() >= 3);
        assertFalse(service.search("password").isEmpty());
    }

    @Test
    void shouldCreateAndFindArticle() {
        ArticleService service = new ArticleService();

        Article article = service.create(
                "Test Article",
                "Test problem",
                "Test solution",
                "tester",
                Article.Status.DRAFT
        );

        assertNotNull(article.getId());
        assertEquals(
                article.getId(),
                service.findById(article.getId()).getId()
        );
    }

    @Test
    void shouldDeleteArticle() {
        ArticleService service = new ArticleService();

        Article article = service.create(
                "Delete Test",
                "Test problem",
                "Test solution",
                "tester",
                Article.Status.DRAFT
        );

        assertTrue(service.delete(article.getId()));
        assertNull(service.findById(article.getId()));
    }
}
