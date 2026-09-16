package com.auctionhouse.dto;

/**
 * DTO for error responses sent back to WebSocket clients.
 */
public class ErrorResponse {
    
    private String message;
    
    public ErrorResponse() {}
    
    public ErrorResponse(String message) {
        this.message = message;
    }
    
    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
