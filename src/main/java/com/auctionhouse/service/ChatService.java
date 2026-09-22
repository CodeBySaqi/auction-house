package com.auctionhouse.service;

import com.auctionhouse.model.ChatMessage;
import com.auctionhouse.model.Conversation;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.ChatMessageRepository;
import com.auctionhouse.repository.ConversationRepository;
import com.auctionhouse.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing chat conversations and messages.
 * Handles creation, sending, access control, and cleanup.
 * Supports both auction-linked and direct message conversations.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MESSAGE_RETENTION_DAYS = 90;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Autowired
    public ChatService(ConversationRepository conversationRepository,
                       ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a conversation for a won auction. Called from PaymentReleaseService.
     * Idempotent — returns existing conversation if one already exists.
     */
    @Transactional
    public Conversation createConversation(PaymentRelease paymentRelease) {
        return conversationRepository.findByPaymentRelease(paymentRelease)
            .orElseGet(() -> {
                Conversation c = new Conversation();
                c.setPaymentRelease(paymentRelease);
                c.setAuction(paymentRelease.getAuction());
                c.setBuyer(paymentRelease.getBuyer());
                c.setSeller(paymentRelease.getSeller());
                c.setDirectMessage(false);
                return conversationRepository.save(c);
            });
    }

    /**
     * Find or create a direct message conversation between two users.
     * Idempotent — returns existing conversation if one already exists.
     */
    @Transactional
    public Conversation findOrCreateDirectConversation(User user1, User user2) {
        if (user1.getId().equals(user2.getId())) {
            throw new IllegalArgumentException("Cannot create a conversation with yourself.");
        }
        return conversationRepository.findDirectConversation(user1.getId(), user2.getId())
            .orElseGet(() -> {
                Conversation c = new Conversation();
                c.setBuyer(user1);
                c.setSeller(user2);
                c.setDirectMessage(true);
                return conversationRepository.save(c);
            });
    }

    /**
     * Send a message. Validates sender is participant.
     * Returns the saved message for broadcasting.
     */
    @Transactional
    public ChatMessage sendMessage(Long conversationId, Long senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // SECURITY: Only buyer or seller can send messages
        if (!sender.getId().equals(conversation.getBuyer().getId())
            && !sender.getId().equals(conversation.getSeller().getId())) {
            throw new SecurityException("You are not a participant in this conversation.");
        }

        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long (max " + MAX_MESSAGE_LENGTH + " characters).");
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        ChatMessage saved = chatMessageRepository.save(message);

        // Update last message timestamp on conversation
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);

        return saved;
    }

    /**
     * Get all messages for a conversation.
     * Validates that the requesting user is a participant or admin.
     */
    public List<ChatMessage> getMessages(Long conversationId, User requestingUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // SECURITY: Only buyer, seller, or admin can read
        boolean isParticipant = requestingUser.getId().equals(conversation.getBuyer().getId())
            || requestingUser.getId().equals(conversation.getSeller().getId());
        boolean isAdmin = "ROLE_ADMIN".equals(requestingUser.getRole())
            || "ROLE_SUPER_ADMIN".equals(requestingUser.getRole());

        if (!isParticipant && !isAdmin) {
            throw new SecurityException("You do not have access to this conversation.");
        }

        return chatMessageRepository.findByConversationId(conversationId);
    }

    /**
     * Get all conversations for a user, ordered by last message time.
     */
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findAllByUserId(userId);
    }

    /**
     * Get the last message in a conversation (for preview in inbox).
     */
    public ChatMessage getLastMessage(Conversation conversation) {
        List<ChatMessage> msgs = chatMessageRepository.findByConversationId(conversation.getId());
        return msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
    }

    /**
     * Count unread messages (messages not sent by the given user, for inbox badge).
     * Simplified: returns total messages in conversation not from this user.
     */
    public long getUnreadCount(Conversation conversation, Long userId) {
        return chatMessageRepository.countMessagesNotFromUser(conversation.getId(), userId);
    }

    /**
     * Get conversation for a given auction, if one exists.
     */
    public Conversation getConversationByAuctionId(Long auctionId) {
        return conversationRepository.findByAuctionId(auctionId).orElse(null);
    }

    /**
     * Get conversation for a given payment release, if one exists.
     */
    public Conversation getConversationByPaymentRelease(PaymentRelease paymentRelease) {
        return conversationRepository.findByPaymentRelease(paymentRelease).orElse(null);
    }

    /**
     * Find conversation by ID.
     */
    public Conversation findById(Long id) {
        return conversationRepository.findById(id).orElse(null);
    }

    /**
     * Get total unread message count for a user (across all conversations).
     */
    public long getTotalUnreadCount(Long userId) {
        return chatMessageRepository.countTotalUnread(userId);
    }

    /**
     * Mark all messages in a conversation as read for the given user.
     */
    @Transactional
    public int markMessagesAsRead(Long conversationId, Long userId) {
        return chatMessageRepository.markAsRead(conversationId, userId);
    }

    /**
     * Delete messages older than 90 days after payment release.
     * Called by scheduled cleanup job.
     */
    @Transactional
    public int cleanupExpiredMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(MESSAGE_RETENTION_DAYS);
        return chatMessageRepository.deleteExpiredMessages(cutoff);
    }

    /**
     * Scheduled task: cleanup expired messages daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void scheduledCleanup() {
        int deleted = cleanupExpiredMessages();
        if (deleted > 0) {
            log.info("Cleaned up {} expired chat messages", deleted);
        }
    }
}
