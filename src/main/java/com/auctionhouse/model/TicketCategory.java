package com.auctionhouse.model;

/**
 * TicketCategory enum - categories for support tickets.
 */
public enum TicketCategory {
    ACCOUNT("Account"),
    PAYMENTS("Payments"),
    TECHNICAL("Technical Issue"),
    TRANSACTIONS("Orders / Transactions"),
    BUG_REPORT("Bug Report"),
    OTHER("Other");

    private final String displayName;

    TicketCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
