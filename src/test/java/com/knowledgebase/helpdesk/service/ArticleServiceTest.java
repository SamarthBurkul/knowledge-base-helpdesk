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

    @Test
    void shouldFilterSearchResultsByStatus() {
        ArticleService service = new ArticleService();

        service.create("Printer Draft", "Printer problem", "Restart the printer",
                "tester", Article.Status.DRAFT);

        var drafts = service.search("printer", Article.Status.DRAFT, null);
        var published = service.search("printer", Article.Status.PUBLISHED, null);

        assertEquals(1, drafts.size());
        assertEquals("Printer Draft", drafts.get(0).getTitle());
        assertTrue(published.isEmpty());
        assertEquals(service.search("printer").size(),
                service.search("printer", null, null).size());
    }

    @Test
    void shouldSortByTitleAndByMostRecentUpdate() {
        ArticleService service = new ArticleService();

        var byTitle = service.search(null, null, "title");
        for (int i = 1; i < byTitle.size(); i++) {
            assertTrue(byTitle.get(i - 1).getTitle()
                    .compareToIgnoreCase(byTitle.get(i).getTitle()) <= 0);
        }

        Article first = service.findAll().get(0);
        service.update(first.getId(), first.getTitle(), first.getDescription(),
                first.getSolution(), first.getStatus());
        first.setUpdatedAt(first.getUpdatedAt().plusMinutes(1));

        assertEquals(first.getId(), service.recentlyUpdated(1).get(0).getId());
    }

    @Test
    void shouldCountArticlesByAuthor() {
        ArticleService service = new ArticleService();

        var counts = service.countByAuthor();

        assertEquals(service.count(),
                counts.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(2L, counts.get("admin"));
    }
}
