package com.auctionhouse.dto;

/**
 * DTO for incoming chat message requests from WebSocket clients.
 */
public class ChatMessageRequest {
    
    private Long conversationId;
    private String content;
    
    public ChatMessageRequest() {}
    
    public ChatMessageRequest(Long conversationId, String content) {
        this.conversationId = conversationId;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
