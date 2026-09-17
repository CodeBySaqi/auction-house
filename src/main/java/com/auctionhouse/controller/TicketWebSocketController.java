package com.auctionhouse.controller;

import com.auctionhouse.dto.ChatMessageDTO;
import com.auctionhouse.dto.ErrorResponse;
import com.auctionhouse.dto.TicketMessageRequest;
import com.auctionhouse.model.TicketMessage;
import com.auctionhouse.model.User;
import com.auctionhouse.service.SupportTicketService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * TicketWebSocketController - real-time ticket messaging using STOMP.
 * 
 * Client sends to: /app/ticket.send
 * Server broadcasts to: /topic/ticket/{ticketId}
 */
@Controller
public class TicketWebSocketController {

    private final SupportTicketService ticketService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public TicketWebSocketController(SupportTicketService ticketService,
                                      UserService userService,
                                      SimpMessagingTemplate messagingTemplate) {
        this.ticketService = ticketService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle incoming STOMP messages for ticket conversations.
     */
    @MessageMapping("/ticket.send")
    public void handleMessage(@Payload TicketMessageRequest request, Principal principal) {
        if (principal == null) {
            return;
        }

        User sender = userService.findByUsername(principal.getName()).orElse(null);
        if (sender == null) {
            return;
        }

        try {
            // Save message (validates permissions, updates status, sends notifications)
            TicketMessage saved = ticketService.sendMessage(
                    request.getTicketId(),
                    sender,
                    request.getContent()
            );

            // Create DTO for broadcasting
            ChatMessageDTO dto = new ChatMessageDTO(
                    saved.getId(),
                    saved.getSender().getUsername(),
                    false, // Client determines isMine
                    saved.getContent(),
                    saved.getCreatedAt()
            );

            // Broadcast to all subscribers of this ticket
            messagingTemplate.convertAndSend(
                    "/topic/ticket/" + request.getTicketId(),
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
