package com.auctionhouse.exception;

/**
 * Custom exception thrown when attempting to bid on a closed auction.
 */
public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) {
        super(message);
    }
}
