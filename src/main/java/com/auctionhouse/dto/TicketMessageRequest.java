package com.auctionhouse.dto;

/**
 * DTO for incoming ticket message requests from WebSocket clients.
 */
public class TicketMessageRequest {

    private Long ticketId;
    private String content;

    public TicketMessageRequest() {}

    public TicketMessageRequest(Long ticketId, String content) {
        this.ticketId = ticketId;
        this.content = content;
    }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
