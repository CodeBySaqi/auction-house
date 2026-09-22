package com.auctionhouse.service;

import com.auctionhouse.model.Broadcast;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.BroadcastRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that checks for pending scheduled broadcasts and sends them
 * when their scheduled time has arrived.
 */
@Component
public class BroadcastScheduler {

    private static final Logger log = LoggerFactory.getLogger(BroadcastScheduler.class);

    private final BroadcastRepository broadcastRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public BroadcastScheduler(BroadcastRepository broadcastRepository,
                              UserService userService,
                              NotificationService notificationService) {
        this.broadcastRepository = broadcastRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    /**
     * Check every minute for scheduled broadcasts that are due.
     */
    @Scheduled(fixedRate = 60000)
    public void processScheduledBroadcasts() {
        List<Broadcast> scheduled = broadcastRepository.findScheduledBroadcasts();
        LocalDateTime now = LocalDateTime.now();

        for (Broadcast broadcast : scheduled) {
            if (broadcast.getScheduledFor() != null && !broadcast.getScheduledFor().isAfter(now)) {
                try {
                    sendScheduledBroadcast(broadcast);
                } catch (Exception e) {
                    log.error("Failed to send scheduled broadcast {}: {}", broadcast.getId(), e.getMessage());
                }
            }
        }
    }

    private void sendScheduledBroadcast(Broadcast broadcast) {
        // Build the notification message
        String typePrefix = switch (broadcast.getType() != null ? broadcast.getType() : "announcement") {
            case "alert" -> "⚠️ ";
            case "update" -> "🔄 ";
            case "new_feature" -> "✨ ";
            default -> "📢 ";
        };
        String fullMessage = typePrefix + "**" + broadcast.getSubject() + "**\n\n" + broadcast.getMessage();

        // Determine recipients
        List<User> allUsers = userService.findAllUsers();
        List<User> recipients;

        switch (broadcast.getAudience()) {
            case "active":
                recipients = allUsers.stream().filter(User::isActive).toList();
                break;
            case "custom":
                // For custom audience, we'd need stored user IDs — for now send to all
                recipients = allUsers;
                break;
            default:
                recipients = allUsers;
                break;
        }

        int sent = 0;
        for (User u : recipients) {
            notificationService.createNotification(u, fullMessage, "ANNOUNCEMENT", null);
            sent++;
        }

        // Update the broadcast record
        broadcast.setScheduled(false);
        broadcast.setSentAt(LocalDateTime.now());
        broadcast.setRecipientCount(sent);
        broadcastRepository.save(broadcast);

        log.info("Sent scheduled broadcast '{}' to {} users", broadcast.getSubject(), sent);
    }
}
