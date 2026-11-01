package com.knowledgebase.helpdesk.controller;

import com.knowledgebase.helpdesk.model.AppUser;
import com.knowledgebase.helpdesk.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute("username") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        AppUser user = authService.authenticate(
                username == null ? "" : username.trim(),
                password == null ? "" : password);

        if (user == null) {
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("username", username);
            return "login";
        }

        // Issue a new session ID on login to prevent session fixation.
        request.changeSessionId();
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session,
                         RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("toast", "You have been signed out.");
        redirectAttributes.addFlashAttribute("toastType", "success");
        return "redirect:/login";
    }
}
