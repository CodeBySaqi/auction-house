package com.auctionhouse.model;

/**
 * TicketStatus enum - lifecycle of a support ticket.
 * OPEN → IN_PROGRESS → RESOLVED → CLOSED
 * RESOLVED can go back to OPEN (user reopens).
 */
public enum TicketStatus {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
