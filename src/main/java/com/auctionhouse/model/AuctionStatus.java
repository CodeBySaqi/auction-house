package com.auctionhouse.model;

/**
 * Enum representing the status of an auction.
 */
public enum AuctionStatus {
    PENDING_APPROVAL,  // Newly created, awaiting admin review
    ACTIVE,            // Approved and live
    REJECTED,          // Rejected by admin
    CLOSED,            // Auction ended
    CANCELLED          // Cancelled by seller or admin
}
