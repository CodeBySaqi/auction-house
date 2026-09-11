package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.Bid;
import com.auctionhouse.model.User;
import com.auctionhouse.service.AuctionService;
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
 */
@Controller
@RequestMapping("/bid")
public class BidController {

    private final BidService bidService;
    private final AuctionService auctionService;
    private final UserService userService;

    @Autowired
    public BidController(BidService bidService, AuctionService auctionService, UserService userService) {
        this.bidService = bidService;
        this.auctionService = auctionService;
        this.userService = userService;
    }

    @PostMapping("/place")
    public String placeBid(@RequestParam Long auctionId,
                           @RequestParam double amount,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            User bidder = userService.getCurrentUser(userDetails.getUsername());
            Auction auction = auctionService.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("Auction not found"));

            Bid bid = bidService.placeBid(auction, bidder, amount);

            // Also save the updated auction
            auctionService.save(auction);

            redirectAttributes.addFlashAttribute("success",
                    String.format("Bid of $%,.2f placed successfully!", amount));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/auctions/detail/" + auctionId;
    }
}
