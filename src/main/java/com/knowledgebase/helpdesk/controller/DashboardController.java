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

        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("articleCount", articleService.count());
        model.addAttribute("publishedCount",
                articleService.countByStatus(Article.Status.PUBLISHED));
        model.addAttribute("draftCount",
                articleService.countByStatus(Article.Status.DRAFT));
        model.addAttribute("resolvedCount",
                articleService.countByStatus(Article.Status.RESOLVED));
        model.addAttribute("userCount", authService.countUsers());

        return "dashboard";
    }
}
