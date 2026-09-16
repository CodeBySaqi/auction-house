package com.auctionhouse.controller;

import com.auctionhouse.model.Notification;
import com.auctionhouse.model.User;
import com.auctionhouse.service.NotificationService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for notifications — used by the navbar popover.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationApiController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        User user = userService.getCurrentUser(userDetails.getUsername());
        List<Notification> all = notificationService.getUserNotifications(user.getId());

        // Take only the 8 most recent
        List<Map<String, Object>> recent = all.stream()
                .limit(8)
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("message", n.getMessage());
                    map.put("type", n.getType());
                    map.put("read", n.isRead());
                    map.put("timeAgo", n.getTimeAgo());
                    map.put("auctionId", n.getAuctionId());
                    return map;
                })
                .collect(Collectors.toList());

        long unreadCount = notificationService.getUnreadCount(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", recent);
        response.put("unreadCount", unreadCount);
        response.put("totalCount", all.size());

        return ResponseEntity.ok(response);
    }
}
