package com.knowledgebase.helpdesk.service;

import com.knowledgebase.helpdesk.model.AppUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final Map<String, AppUser> users;

    public AuthService() {
        users = Map.of(
                "admin", new AppUser(
                        "admin",
                        encoder.encode("admin123"),
                        "ADMIN"
                ),
                "support", new AppUser(
                        "support",
                        encoder.encode("support123"),
                        "SUPPORT"
                ),
                "user", new AppUser(
                        "user",
                        encoder.encode("user123"),
                        "USER"
                )
        );
    }

    public AppUser authenticate(String username, String password) {

        AppUser user = users.get(username);

        if (user != null && encoder.matches(password, user.getPassword())) {
            return user;
        }

        return null;
    }

    public long countUsers() {
        return users.size();
    }
}
