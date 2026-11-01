package com.knowledgebase.helpdesk.controller;

import com.knowledgebase.helpdesk.model.Article;
import com.knowledgebase.helpdesk.service.ArticleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    static final int TITLE_MAX = 150;
    static final int DESCRIPTION_MAX = 2000;
    static final int SOLUTION_MAX = 10000;

    /**
     * Drafts are work in progress: only authors (ADMIN, SUPPORT) may see them.
     * This is enforced here on the server, not only by hiding links in the UI.
     */
    private boolean canSee(HttpSession session, Article article) {
        return article != null
                && (canManage(session) || article.getStatus() != Article.Status.DRAFT);
    }

    private static void toast(RedirectAttributes redirectAttributes,
                              String message, String type) {
        redirectAttributes.addFlashAttribute("toast", message);
        redirectAttributes.addFlashAttribute("toastType", type);
    }

    private static String denied(RedirectAttributes redirectAttributes) {
        toast(redirectAttributes,
                "You don't have permission to perform that action.", "warning");
        return "redirect:/articles";
    }

    private static Article.Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Article.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Map<String, String> validate(String title,
                                        String description,
                                        String solution,
                                        Article.Status status) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (title == null || title.isBlank()) {
            errors.put("title", "Enter a title for the article.");
        } else if (title.trim().length() > TITLE_MAX) {
            errors.put("title", "Keep the title under " + TITLE_MAX + " characters.");
        }
        if (description == null || description.isBlank()) {
            errors.put("description", "Describe the problem this article solves.");
        } else if (description.trim().length() > DESCRIPTION_MAX) {
            errors.put("description",
                    "Keep the problem description under " + DESCRIPTION_MAX + " characters.");
        }
        if (solution == null || solution.isBlank()) {
            errors.put("solution", "Provide the solution steps.");
        } else if (solution.trim().length() > SOLUTION_MAX) {
            errors.put("solution", "Keep the solution under " + SOLUTION_MAX + " characters.");
        }
        if (status == null) {
            errors.put("status", "Choose Draft, Published or Resolved.");
        }
        return errors;
    }

    @GetMapping
    public String articles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String view,
            HttpSession session,
            Model model) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }

        boolean manager = canManage(session);
        Article.Status statusFilter = parseStatus(status);
        if (!manager && statusFilter == Article.Status.DRAFT) {
            statusFilter = null;
        }
        String sortKey = "updated".equals(sort) || "title".equals(sort) ? sort : "";
        List<Article> matches = articleService.search(keyword)
                .stream()
                .filter(a -> canSee(session, a))
                .toList();

        Map<Article.Status, Long> statusCounts = new EnumMap<>(Article.Status.class);
        for (Article.Status s : Article.Status.values()) {
            if (s == Article.Status.DRAFT && !manager) {
                continue;
            }
            statusCounts.put(s, matches.stream().filter(a -> a.getStatus() == s).count());
        }

        String viewMode = "table".equals(view) || "list".equals(view)
                ? view
                : (canManage(session) ? "table" : "list");

        String statusName = statusFilter == null ? null : statusFilter.name();
        Map<String, String> links = new LinkedHashMap<>();
        links.put("ALL", ArticleLinks.list(keyword, null, sortKey, viewMode));
        for (Article.Status s : statusCounts.keySet()) {
            links.put(s.name(), ArticleLinks.list(keyword, s.name(), sortKey, viewMode));
        }
        links.put("list", ArticleLinks.list(keyword, statusName, sortKey, "list"));
        links.put("table", ArticleLinks.list(keyword, statusName, sortKey, "table"));
        links.put("clear", ArticleLinks.list(null, null, sortKey, viewMode));

        model.addAttribute("links", links);
        model.addAttribute("articles", articleService.search(keyword, statusFilter, sortKey)
                .stream()
                .filter(a -> canSee(session, a))
                .toList());
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("status", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("sort", sortKey);
        model.addAttribute("view", viewMode);
        model.addAttribute("totalMatches", matches.size());
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("role", role(session));

        return "articles";
    }

    @GetMapping("/{id}")
    public String view(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }

        Article article = articleService.findById(id);

        // A regular user asking for a draft gets the same answer as for a
        // missing article, so draft titles and IDs are not disclosed.
        if (!canSee(session, article)) {
            toast(redirectAttributes,
                    "That article could not be found. It may have been deleted.", "warning");
            return "redirect:/articles";
        }

        model.addAttribute("article", article);
        model.addAttribute("role", role(session));

        return "article-view";
    }

    @GetMapping("/new")
    public String newArticle(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }
        if (!canManage(session)) {
            return denied(redirectAttributes);
        }

        model.addAttribute("article", new Article());
        model.addAttribute("action", "/articles/create");
        model.addAttribute("pageTitle", "Create Knowledge Article");

        return "article-form";
    }

    @PostMapping("/create")
    public String create(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String solution,
            @RequestParam(name = "status", required = false) String statusParam,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }
        if (!canManage(session)) {
            return denied(redirectAttributes);
        }

        Article.Status status = parseStatus(statusParam);
        Map<String, String> errors = validate(title, description, solution, status);
        if (!errors.isEmpty()) {
            Article draft = new Article();
            draft.setTitle(title);
            draft.setDescription(description);
            draft.setSolution(solution);
            draft.setStatus(status);

            model.addAttribute("article", draft);
            model.addAttribute("action", "/articles/create");
            model.addAttribute("pageTitle", "Create Knowledge Article");
            model.addAttribute("errors", errors);
            return "article-form";
        }

        Article article = articleService.create(
                title.trim(),
                description.trim(),
                solution.trim(),
                session.getAttribute("username").toString(),
                status
        );

        toast(redirectAttributes, "Article created successfully.", "success");
        return "redirect:/articles/" + article.getId();
    }

    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }
        if (!canManage(session)) {
            return denied(redirectAttributes);
        }

        Article article = articleService.findById(id);

        if (article == null) {
            toast(redirectAttributes,
                    "That article could not be found. It may have been deleted.", "warning");
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
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String solution,
            @RequestParam(name = "status", required = false) String statusParam,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }
        if (!canManage(session)) {
            return denied(redirectAttributes);
        }

        Article existing = articleService.findById(id);
        if (existing == null) {
            toast(redirectAttributes,
                    "That article could not be found. It may have been deleted.", "warning");
            return "redirect:/articles";
        }

        Article.Status status = parseStatus(statusParam);
        Map<String, String> errors = validate(title, description, solution, status);
        if (!errors.isEmpty()) {
            Article draft = new Article(id, title, description, solution,
                    existing.getAuthor(), status);
            draft.setUpdatedAt(existing.getUpdatedAt());

            model.addAttribute("article", draft);
            model.addAttribute("action", "/articles/update/" + id);
            model.addAttribute("pageTitle", "Update Knowledge Article");
            model.addAttribute("errors", errors);
            return "article-form";
        }

        articleService.update(
                id,
                title.trim(),
                description.trim(),
                solution.trim(),
                status
        );

        toast(redirectAttributes, "Changes saved.", "success");
        return "redirect:/articles/" + id;
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!loggedIn(session)) {
            return "redirect:/login";
        }
        if (!"ADMIN".equals(role(session))) {
            return denied(redirectAttributes);
        }

        Article article = articleService.findById(id);

        if (article != null && articleService.delete(id)) {
            toast(redirectAttributes,
                    "Deleted “" + article.getTitle() + "”.", "success");
        } else {
            toast(redirectAttributes,
                    "That article could not be found. It may have already been deleted.",
                    "warning");
        }

        return "redirect:/articles";
    }
}
