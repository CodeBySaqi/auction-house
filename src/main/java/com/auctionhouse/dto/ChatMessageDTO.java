package com.auctionhouse.dto;

import java.time.LocalDateTime;

/**
 * DTO for chat messages - used in API responses.
 * Never exposes full User entity, only necessary fields.
 */
public class ChatMessageDTO {
    
    private Long id;
    private String senderUsername;
    private boolean isMine;
    private String content;
    private LocalDateTime timestamp;
    
    public ChatMessageDTO() {}
    
    public ChatMessageDTO(Long id, String senderUsername, boolean isMine, String content, LocalDateTime timestamp) {
        this.id = id;
        this.senderUsername = senderUsername;
        this.isMine = isMine;
        this.content = content;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    
    public boolean isMine() { return isMine; }
    public void setMine(boolean mine) { isMine = mine; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
