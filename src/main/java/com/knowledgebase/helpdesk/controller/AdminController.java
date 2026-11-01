package com.knowledgebase.helpdesk.controller;

import com.knowledgebase.helpdesk.service.ArticleService;
import com.knowledgebase.helpdesk.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final AuthService authService;
    private final ArticleService articleService;

    public AdminController(AuthService authService,
                           ArticleService articleService) {
        this.authService = authService;
        this.articleService = articleService;
    }

    @GetMapping("/admin/users")
    public String users(HttpSession session,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("toast",
                    "User management is available to administrators only.");
            redirectAttributes.addFlashAttribute("toastType", "warning");
            return "redirect:/dashboard";
        }

        model.addAttribute("users", authService.findAllUsers());
        model.addAttribute("authoredCounts", articleService.countByAuthor());

        return "admin-users";
    }
}
