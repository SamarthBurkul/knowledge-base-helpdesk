package com.knowledgebase.helpdesk.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the signed-in user to every view so the shared layout
 * (sidebar, top bar, profile menu) can render role-aware navigation.
 * Authorization itself stays in the individual controllers.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUser")
    public String currentUser(HttpSession session) {
        Object username = session.getAttribute("username");
        return username == null ? null : username.toString();
    }

    @ModelAttribute("currentRole")
    public String currentRole(HttpSession session) {
        Object role = session.getAttribute("role");
        return role == null ? "" : role.toString();
    }
}
