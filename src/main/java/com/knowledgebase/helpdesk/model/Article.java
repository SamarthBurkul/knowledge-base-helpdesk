package com.knowledgebase.helpdesk.model;

import java.time.LocalDateTime;

public class Article {

    public enum Status {
        DRAFT,
        PUBLISHED,
        RESOLVED
    }

    private Long id;
    private String title;
    private String description;
    private String solution;
    private String author;
    private Status status;
    private LocalDateTime updatedAt;

    public Article() {
    }

    public Article(Long id, String title, String description,
                   String solution, String author, Status status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.solution = solution;
        this.author = author;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
