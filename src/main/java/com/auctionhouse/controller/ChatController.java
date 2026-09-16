package com.auctionhouse.controller;

import com.auctionhouse.dto.ChatMessageDTO;
import com.auctionhouse.model.ChatMessage;
import com.auctionhouse.model.User;
import com.auctionhouse.service.ChatService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP controller for chat operations.
 * Provides REST endpoints for loading message history.
 */
@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /**
     * GET /chat/{conversationId}/messages
     * Returns message history as JSON (for initial load + AJAX refresh fallback)
     */
    @GetMapping("/{conversationId}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByUsername(userDetails.getUsername())
            .orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<ChatMessage> messages = chatService.getMessages(conversationId, user);

            // Map to DTO (never expose full User entity)
            List<ChatMessageDTO> dtos = messages.stream()
                .map(m -> new ChatMessageDTO(
                    m.getId(),
                    m.getSender().getUsername(),
                    m.getSender().getId().equals(user.getId()), // isMine
                    m.getContent(),
                    m.getCreatedAt()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).build();
        }
    }
}
