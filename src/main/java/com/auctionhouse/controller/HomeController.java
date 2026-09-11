package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * HomeController - handles the homepage.
 */
@Controller
public class HomeController {

    private final AuctionService auctionService;

    @Autowired
    public HomeController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        List<Auction> featured = auctionService.getFeaturedAuctions();
        long activeCount = auctionService.getActiveCount();
        model.addAttribute("featuredAuctions", featured);
        model.addAttribute("activeCount", activeCount);
        return "index";
    }
}
