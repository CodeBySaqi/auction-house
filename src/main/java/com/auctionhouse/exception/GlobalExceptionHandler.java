package com.auctionhouse.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuctionClosedException.class)
    public String handleAuctionClosed(AuctionClosedException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "redirect:/auctions?error=auction_closed";
    }

    @ExceptionHandler(BidTooLowException.class)
    public String handleBidTooLow(BidTooLowException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "redirect:/auctions?error=bid_too_low";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Unhandled exception", ex);
        model.addAttribute("error", "An unexpected error occurred. Please try again or contact support.");
        return "error";
    }
}
