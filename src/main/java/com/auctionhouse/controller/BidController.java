package com.auctionhouse.controller;

import com.auctionhouse.model.Bid;
import com.auctionhouse.model.User;
import com.auctionhouse.service.BidService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * BidController - handles bid placement.
 * Only authenticated users can place bids.
 */
@Controller
@RequestMapping("/bid")
public class BidController {

    private final BidService bidService;
    private final UserService userService;

    @Autowired
    public BidController(BidService bidService, UserService userService) {
        this.bidService = bidService;
        this.userService = userService;
    }

    @PostMapping("/place")
    public String placeBid(@RequestParam Long auctionId,
                           @RequestParam double amount,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        // Validation: User must be authenticated
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to place a bid.");
            return "redirect:/login";
        }

        try {
            User bidder = userService.getCurrentUser(userDetails.getUsername());
            
            // Place the bid (all validation happens in service layer)
            Bid bid = bidService.placeBid(auctionId, bidder, amount);

            redirectAttributes.addFlashAttribute("success",
                    String.format("Bid of $%,.2f placed successfully!", bid.getAmount()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/auctions/detail/" + auctionId;
    }
}
