package com.auctionhouse.exception;

/**
 * Custom exception thrown when a bid is too low.
 */
public class BidTooLowException extends RuntimeException {
    public BidTooLowException(String message) {
        super(message);
    }
}
