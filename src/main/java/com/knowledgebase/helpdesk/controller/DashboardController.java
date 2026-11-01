package com.knowledgebase.helpdesk.controller;

import com.knowledgebase.helpdesk.model.Article;
import com.knowledgebase.helpdesk.service.ArticleService;
import com.knowledgebase.helpdesk.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ArticleService articleService;
    private final AuthService authService;

    public DashboardController(ArticleService articleService,
                               AuthService authService) {
        this.articleService = articleService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {

        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }

        Object role = session.getAttribute("role");
        boolean canManage = "ADMIN".equals(role) || "SUPPORT".equals(role);

        long published = articleService.countByStatus(Article.Status.PUBLISHED);
        long resolved = articleService.countByStatus(Article.Status.RESOLVED);
        // Regular users never see drafts, so they are left out of their figures.
        long drafts = canManage ? articleService.countByStatus(Article.Status.DRAFT) : 0;

        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("role", role);
        model.addAttribute("articleCount", published + resolved + drafts);
        model.addAttribute("publishedCount", published);
        model.addAttribute("draftCount", drafts);
        model.addAttribute("resolvedCount", resolved);
        model.addAttribute("recentArticles", canManage
                ? articleService.recentlyUpdated(5)
                : articleService.search(null, null, "updated").stream()
                        .filter(a -> a.getStatus() != Article.Status.DRAFT)
                        .limit(5)
                        .toList());

        if ("ADMIN".equals(role) || "SUPPORT".equals(role)) {
            model.addAttribute("draftArticles",
                    articleService.findByStatus(Article.Status.DRAFT));
        }

        if ("ADMIN".equals(role)) {
            model.addAttribute("userCount", authService.countUsers());
            model.addAttribute("users", authService.findAllUsers());
        }

        return "dashboard";
    }
}
