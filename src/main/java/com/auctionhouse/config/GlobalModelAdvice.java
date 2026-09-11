package com.auctionhouse.config;

import com.auctionhouse.model.User;
import com.auctionhouse.service.NotificationService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

/**
 * Global model attributes added to every page.
 * Fixes the bug where navbar variables like unreadCount weren't available on all pages.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public GlobalModelAdvice(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadCount")
    public long unreadCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                Optional<User> user = userService.findByUsername(auth.getName());
                if (user.isPresent()) {
                    return notificationService.getUnreadCount(user.get().getId());
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return 0;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                return userService.findByUsername(auth.getName()).orElse(null);
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }
}
