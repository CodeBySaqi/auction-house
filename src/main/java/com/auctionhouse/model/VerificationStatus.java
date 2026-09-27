package com.auctionhouse.model;

/**
 * Verification status for payment release workflow.
 * Tracks the lifecycle from auction win to payment release.
 */
public enum VerificationStatus {
    WAITING_FOR_DETAILS,          // Initial state - waiting for both parties
    SELLER_DETAILS_SUBMITTED,     // Seller has submitted delivery details
    BUYER_DETAILS_SUBMITTED,      // Buyer has submitted received-item details
    READY_FOR_ADMIN_REVIEW,       // Both parties submitted, ready for admin
    VERIFIED,                     // Admin verified the case
    PAYMENT_RELEASED,             // Payment released to seller
    REJECTED,                     // Admin rejected/needs correction (legacy, both)
    NEEDS_CORRECTION,             // Admin requested corrections from both
    SELLER_CORRECTION_NEEDED,     // Admin requested correction from seller only
    BUYER_CORRECTION_NEEDED       // Admin requested correction from buyer only
}
