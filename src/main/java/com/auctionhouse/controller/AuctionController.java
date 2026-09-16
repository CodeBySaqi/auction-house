package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionCategory;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.Bid;
import com.auctionhouse.model.CollectibleAuction;
import com.auctionhouse.model.User;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.BidService;
import com.auctionhouse.service.FileStorageService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuctionController - handles auction listing and detail views.
 */
@Controller
@RequestMapping("/auctions")
public class AuctionController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Autowired
    public AuctionController(AuctionService auctionService, BidService bidService, UserService userService, FileStorageService fileStorageService) {
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
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
    public String auctionDetail(@PathVariable Long id, Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Restrict access to pending/rejected auctions - only owner or admin can view
        if (auction.getStatus() == AuctionStatus.PENDING_APPROVAL || 
            auction.getStatus() == AuctionStatus.REJECTED) {
            boolean canView = false;
            if (userDetails != null) {
                User currentUser = userService.getCurrentUser(userDetails.getUsername());
                // Owner can view
                if (auction.getCreatedBy() != null && auction.getCreatedBy().getId().equals(currentUser.getId())) {
                    canView = true;
                }
                // Admin can view
                if ("ROLE_ADMIN".equals(currentUser.getRole()) || "ROLE_SUPER_ADMIN".equals(currentUser.getRole())) {
                    canView = true;
                }
            }
            if (!canView) {
                return "redirect:/auctions";
            }
        }

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

    @GetMapping("/create")
    public String createAuctionPage(Model model) {
        model.addAttribute("categories", AuctionCategory.values());
        return "auctions/create";
    }

    @PostMapping("/create")
    public String createAuction(@RequestParam String title,
                                @RequestParam String description,
                                @RequestParam double startingPrice,
                                @RequestParam String category,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile auctionImage,
                                @RequestParam int durationMinutes,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User creator = userService.getCurrentUser(userDetails.getUsername());
            AuctionCategory cat = AuctionCategory.valueOf(category.toUpperCase());

            // Determine image URL: uploaded file takes priority over URL
            String finalImageUrl = imageUrl;
            if (auctionImage != null && !auctionImage.isEmpty()) {
                String filename = fileStorageService.storeAuctionImage(auctionImage);
                finalImageUrl = "/uploads/auctions/" + filename;
            }

            // Fallback image if neither provided
            if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
                finalImageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800";
            }

            // Create a CollectibleAuction for user-created items
            CollectibleAuction auction = new CollectibleAuction();
            auction.setTitle(title);
            auction.setDescription(description);
            auction.setStartingPrice(startingPrice);
            auction.setImageUrl(finalImageUrl);
            auction.setCategory(cat);
            auction.setEndTime(LocalDateTime.now().plusMinutes(durationMinutes));
            auction.setCreatedBy(creator);
            auction.setSubcategory(cat.getDisplayName());
            auction.setEra("2026");
            auction.setCondition("As described");
            auction.setRarity("User Listed");
            // Status is already PENDING_APPROVAL by default from Auction constructor

            auctionService.save(auction);
            redirectAttributes.addFlashAttribute("success", "Your auction has been submitted for review! You'll be notified once it's approved. 📋");
            return "redirect:/my-auctions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create auction: " + e.getMessage());
            return "redirect:/auctions/create";
        }
    }
}
