package com.auctionhouse.service;

import com.auctionhouse.model.Notification;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NotificationService - manages user notifications.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create a new notification for a user.
     */
    @Transactional
    public Notification createNotification(User user, String message, String type, Long auctionId) {
        // Defensive truncation: never let an oversized message break persistence
        // (column is varchar(1200)). Protects every caller, not just broadcasts.
        if (message != null && message.length() > 1200) {
            message = message.substring(0, 1197) + "...";
        }
        Notification notification = new Notification(user, message, type, auctionId);
        return notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a user.
     */
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Get count of unread notifications.
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByTimestampDesc(userId);
        for (Notification n : notifications) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}
