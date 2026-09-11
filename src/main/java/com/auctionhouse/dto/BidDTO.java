package com.auctionhouse.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

/**
 * DTO for bid submission form data.
 */
public class BidDTO {

    @NotNull(message = "Auction ID is required")
    private Long auctionId;

    @DecimalMin(value = "0.01", message = "Bid amount must be positive")
    private double amount;

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
