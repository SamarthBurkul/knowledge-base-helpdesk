package com.knowledgebase.helpdesk.controller;

import com.knowledgebase.helpdesk.model.Article;
import com.knowledgebase.helpdesk.service.ArticleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    private boolean loggedIn(HttpSession session) {
        return session.getAttribute("username") != null;
    }

    private String role(HttpSession session) {
        Object role = session.getAttribute("role");
        return role == null ? "" : role.toString();
    }

    private boolean canManage(HttpSession session) {
        return "ADMIN".equals(role(session)) || "SUPPORT".equals(role(session));
    }

    @GetMapping
    public String articles(
            @RequestParam(required = false) String keyword,
            HttpSession session,
            Model model) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }

        model.addAttribute("articles", articleService.search(keyword));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("role", role(session));

        return "articles";
    }

    @GetMapping("/{id}")
    public String view(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }

        Article article = articleService.findById(id);

        if (article == null) {
            return "redirect:/articles";
        }

        model.addAttribute("article", article);
        model.addAttribute("role", role(session));

        return "article-view";
    }

    @GetMapping("/new")
    public String newArticle(
            HttpSession session,
            Model model) {

        if (!loggedIn(session) || !canManage(session)) {
            return "redirect:/articles";
        }

        model.addAttribute("article", new Article());
        model.addAttribute("action", "/articles/create");
        model.addAttribute("pageTitle", "Create Knowledge Article");

        return "article-form";
    }

    @PostMapping("/create")
    public String create(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String solution,
            @RequestParam Article.Status status,
            HttpSession session) {

        if (!loggedIn(session) || !canManage(session)) {
            return "redirect:/articles";
        }

        articleService.create(
                title,
                description,
                solution,
                session.getAttribute("username").toString(),
                status
        );

        return "redirect:/articles";
    }

    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        if (!loggedIn(session) || !canManage(session)) {
            return "redirect:/articles";
        }

        Article article = articleService.findById(id);

        if (article == null) {
            return "redirect:/articles";
        }

        model.addAttribute("article", article);
        model.addAttribute("action", "/articles/update/" + id);
        model.addAttribute("pageTitle", "Update Knowledge Article");

        return "article-form";
    }

    @PostMapping("/update/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String solution,
            @RequestParam Article.Status status,
            HttpSession session) {

        if (!loggedIn(session) || !canManage(session)) {
            return "redirect:/articles";
        }

        articleService.update(
                id,
                title,
                description,
                solution,
                status
        );

        return "redirect:/articles/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            HttpSession session) {

        if (!loggedIn(session) || !"ADMIN".equals(role(session))) {
            return "redirect:/articles";
        }

        articleService.delete(id);

        return "redirect:/articles";
    }
}
