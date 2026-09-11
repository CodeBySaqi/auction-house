package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionCategory;
import com.auctionhouse.model.Bid;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * AuctionController - handles auction listing and detail views.
 */
@Controller
@RequestMapping("/auctions")
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;

    @Autowired
    public AuctionController(AuctionService auctionService, BidService bidService) {
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    @GetMapping
    public String listAuctions(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String search,
                               Model model) {
        List<Auction> auctions;

        if (search != null && !search.trim().isEmpty()) {
            auctions = auctionService.searchAuctions(search);
            model.addAttribute("searchQuery", search);
        } else if (category != null && !category.trim().isEmpty()) {
            try {
                AuctionCategory cat = AuctionCategory.valueOf(category.toUpperCase());
                auctions = auctionService.getActiveAuctionsByCategory(cat);
            } catch (IllegalArgumentException e) {
                auctions = auctionService.getActiveAuctions();
            }
            model.addAttribute("selectedCategory", category);
        } else {
            auctions = auctionService.getActiveAuctions();
        }

        model.addAttribute("auctions", auctions);
        model.addAttribute("categories", AuctionCategory.values());
        return "auctions/list";
    }

    @GetMapping("/detail/{id}")
    public String auctionDetail(@PathVariable Long id, Model model) {
        Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        List<Bid> bids = bidService.getBidsByAuction(id);
        double minimumBid = auction.getMinimumBid();

        model.addAttribute("auction", auction);
        model.addAttribute("bids", bids);
        model.addAttribute("minimumBid", minimumBid);
        return "auctions/detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam String q, Model model) {
        return "redirect:/auctions?search=" + q;
    }
}
