package com.auctionhouse.controller;

import com.auctionhouse.dto.ChatMessageDTO;
import com.auctionhouse.dto.ChatMessageRequest;
import com.auctionhouse.dto.ErrorResponse;
import com.auctionhouse.model.ChatMessage;
import com.auctionhouse.model.User;
import com.auctionhouse.service.ChatService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time chat using STOMP.
 * 
 * Client sends to: /app/chat.send
 * Server broadcasts to: /topic/chat/{conversationId}
 */
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatWebSocketController(ChatService chatService,
                                    UserService userService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle incoming STOMP messages.
     * Validates sender, saves message, and broadcasts to conversation subscribers.
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatMessageRequest request,
                               Principal principal) {
        if (principal == null) {
            return; // Not authenticated
        }

        User sender = userService.findByUsername(principal.getName()).orElse(null);
        if (sender == null) {
            return;
        }

        try {
            // Save message (validates permissions)
            ChatMessage saved = chatService.sendMessage(
                request.getConversationId(),
                sender.getId(),
                request.getContent()
            );

            // Create DTO for broadcasting
            ChatMessageDTO dto = new ChatMessageDTO(
                saved.getId(),
                saved.getSender().getUsername(),
                false, // Client will determine isMine based on username match
                saved.getContent(),
                saved.getCreatedAt()
            );

            // Broadcast to all subscribers of this conversation
            messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getConversationId(),
                dto
            );
        } catch (SecurityException | IllegalArgumentException e) {
            // Send error back to sender only
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                new ErrorResponse(e.getMessage())
            );
        }
    }
}
