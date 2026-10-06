package com.knowledgebase.helpdesk.service;

import com.knowledgebase.helpdesk.model.Article;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ArticleService {

    private final ConcurrentHashMap<Long, Article> articles = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    public ArticleService() {
        create("Password Reset Guide",
                "User cannot remember their password.",
                "Open the password reset page and follow the verification steps.",
                "admin",
                Article.Status.PUBLISHED);

        create("Wi-Fi Troubleshooting",
                "User cannot connect to organization Wi-Fi.",
                "Check credentials, restart Wi-Fi and reconnect to the correct network.",
                "admin",
                Article.Status.PUBLISHED);

        create("Application Access Request",
                "User requires access to an internal application.",
                "Submit an access request to the support team with manager approval.",
                "support",
                Article.Status.PUBLISHED);
    }

    public Article create(String title, String description,
                          String solution, String author, Article.Status status) {

        Long id = idGenerator.incrementAndGet();

        Article article = new Article(
                id,
                title,
                description,
                solution,
                author,
                status
        );

        articles.put(id, article);
        return article;
    }

    public List<Article> findAll() {
        return articles.values()
                .stream()
                .sorted(Comparator.comparing(Article::getId))
                .toList();
    }

    public List<Article> search(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        String search = keyword.toLowerCase();

        return articles.values()
                .stream()
                .filter(article ->
                        article.getTitle().toLowerCase().contains(search)
                                || article.getDescription().toLowerCase().contains(search)
                                || article.getSolution().toLowerCase().contains(search))
                .sorted(Comparator.comparing(Article::getId))
                .toList();
    }

    public Article findById(Long id) {
        return articles.get(id);
    }

    public boolean update(Long id,
                          String title,
                          String description,
                          String solution,
                          Article.Status status) {

        Article article = articles.get(id);

        if (article == null) {
            return false;
        }

        article.setTitle(title);
        article.setDescription(description);
        article.setSolution(solution);
        article.setStatus(status);
        article.setUpdatedAt(LocalDateTime.now());

        return true;
    }

    public boolean delete(Long id) {
        return articles.remove(id) != null;
    }

    public long count() {
        return articles.size();
    }

    public long countByStatus(Article.Status status) {
        return articles.values()
                .stream()
                .filter(article -> article.getStatus() == status)
                .count();
    }
}
