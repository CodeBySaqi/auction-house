package com.auctionhouse.controller;

import com.auctionhouse.model.ChatMessage;
import com.auctionhouse.model.Conversation;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.UserRepository;
import com.auctionhouse.service.ChatService;
import com.auctionhouse.service.NotificationService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for user-to-user direct messaging.
 * Provides inbox, thread view, and user search endpoints.
 */
@Controller
@RequestMapping("/messages")
@PreAuthorize("isAuthenticated()")
public class MessageController {

    private final ChatService chatService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public MessageController(ChatService chatService,
                             UserService userService,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.chatService = chatService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * GET /messages — Inbox page showing all conversations
     */
    @GetMapping({"", "/"})
    public String inbox(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        List<Conversation> conversations = chatService.getUserConversations(user.getId());

        // Build conversation preview data
        List<Map<String, Object>> conversationPreviews = new ArrayList<>();
        for (Conversation conv : conversations) {
            Map<String, Object> preview = new HashMap<>();
            preview.put("conversation", conv);
            preview.put("otherUser", conv.getOtherParticipant(user));
            preview.put("lastMessage", chatService.getLastMessage(conv));
            conversationPreviews.add(preview);
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("conversations", conversationPreviews);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        model.addAttribute("pageTitle", "Messages");
        return "messages-inbox";
    }

    /**
     * GET /messages/{id} — Thread view for a specific conversation
     */
    @GetMapping("/{id}")
    public String thread(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        Conversation conversation = chatService.findById(id);
        if (conversation == null) {
            return "redirect:/messages";
        }

        // Security: Only participants can view
        boolean isParticipant = user.getId().equals(conversation.getBuyer().getId())
            || user.getId().equals(conversation.getSeller().getId());
        if (!isParticipant) {
            return "redirect:/messages";
        }

        User otherUser = conversation.getOtherParticipant(user);
        List<ChatMessage> messages = chatService.getMessages(id, user);

        model.addAttribute("currentUser", user);
        model.addAttribute("conversation", conversation);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("messages", messages);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
        model.addAttribute("pageTitle", "Chat with " + (otherUser != null ? otherUser.getUsername() : "User"));
        return "messages-thread";
    }

    /**
     * POST /messages/start/{username} — Start or open a DM with a user
     */
    @PostMapping("/start/{username}")
    public String startConversation(@PathVariable String username, Principal principal, RedirectAttributes ra) {
        User currentUser = userService.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        User otherUser = userService.findByUsername(username).orElse(null);
        if (otherUser == null) {
            ra.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/messages";
        }

        if (currentUser.getId().equals(otherUser.getId())) {
            ra.addFlashAttribute("errorMessage", "You cannot message yourself.");
            return "redirect:/messages";
        }

        try {
            Conversation conv = chatService.findOrCreateDirectConversation(currentUser, otherUser);
            return "redirect:/messages/" + conv.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Could not start conversation: " + e.getMessage());
            return "redirect:/messages";
        }
    }

    /**
     * GET /messages/search?q=... — AJAX endpoint to search users for starting new conversations
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> searchUsers(@RequestParam("q") String query, Principal principal) {
        User currentUser = userService.findByUsername(principal.getName()).orElse(null);
        if (currentUser == null) return ResponseEntity.status(401).build();

        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<User> users = userRepository.searchUsers(query.trim());

        // Filter out current user, limit to 10, exclude admins
        List<Map<String, String>> results = users.stream()
            .filter(u -> !u.getId().equals(currentUser.getId()))
            .filter(u -> u.isActive())
            .limit(10)
            .map(u -> {
                Map<String, String> map = new HashMap<>();
                map.put("username", u.getUsername());
                map.put("role", u.getRole() != null ? u.getRole().replace("ROLE_", "") : "USER");
                return map;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    /**
     * GET /messages/recent — AJAX endpoint returning recent conversations for the floating popover
     */
    @GetMapping("/recent")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recentConversations(Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        List<Conversation> conversations = chatService.getUserConversations(user.getId());

        List<Map<String, Object>> items = new ArrayList<>();
        int limit = Math.min(conversations.size(), 5);
        for (int i = 0; i < limit; i++) {
            Conversation conv = conversations.get(i);
            User other = conv.getOtherParticipant(user);
            ChatMessage lastMsg = chatService.getLastMessage(conv);

            Map<String, Object> item = new HashMap<>();
            item.put("id", conv.getId());
            item.put("username", other != null ? other.getUsername() : "Unknown");
            item.put("initial", other != null && !other.getUsername().isEmpty()
                    ? other.getUsername().substring(0, 1).toUpperCase() : "?");
            item.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "No messages yet");
            item.put("time", lastMsg != null
                    ? formatTimeAgo(lastMsg.getCreatedAt())
                    : formatTimeAgo(conv.getCreatedAt()));
            item.put("isDirect", conv.isDirectMessage());
            items.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("conversations", items);
        result.put("total", conversations.size());
        result.put("unreadCount", chatService.getTotalUnreadCount(user.getId()));
        return ResponseEntity.ok(result);
    }

    /**
     * POST /messages/{id}/read — Mark all messages in a conversation as read (AJAX)
     */
    @PostMapping("/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        Conversation conversation = chatService.findById(id);
        if (conversation == null) return ResponseEntity.status(404).build();

        boolean isParticipant = user.getId().equals(conversation.getBuyer().getId())
            || user.getId().equals(conversation.getSeller().getId());
        if (!isParticipant) return ResponseEntity.status(403).build();

        int marked = chatService.markMessagesAsRead(id, user.getId());

        Map<String, Object> resp = new HashMap<>();
        resp.put("marked", marked);
        resp.put("unreadCount", chatService.getTotalUnreadCount(user.getId()));
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /messages/unread-count — Lightweight endpoint for badge polling
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        Map<String, Long> resp = new HashMap<>();
        resp.put("unreadCount", chatService.getTotalUnreadCount(user.getId()));
        return ResponseEntity.ok(resp);
    }

    private String formatTimeAgo(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        java.time.Duration d = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        if (d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + "m";
        if (d.toHours() < 24) return d.toHours() + "h";
        if (d.toDays() < 7) return d.toDays() + "d";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
    }
}
