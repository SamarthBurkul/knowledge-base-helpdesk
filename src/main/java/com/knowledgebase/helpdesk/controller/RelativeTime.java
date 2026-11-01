package com.knowledgebase.helpdesk.controller;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formats timestamps relative to the server clock, so the wording matches
 * the clock that produced {@code Article.updatedAt}. Used from templates as
 * {@code ${@relativeTime.format(article.updatedAt)}}.
 */
@Component("relativeTime")
public class RelativeTime {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    public String format(LocalDateTime time) {
        if (time == null) {
            return "";
        }

        Duration age = Duration.between(time, LocalDateTime.now());
        long minutes = age.toMinutes();

        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        long hours = age.toHours();
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = age.toDays();
        if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        }
        return time.format(DATE);
    }
}
