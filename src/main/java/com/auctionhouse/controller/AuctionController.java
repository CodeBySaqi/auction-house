package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionCategory;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.ArtAuction;
import com.auctionhouse.model.Bid;
import com.auctionhouse.model.CarAuction;
import com.auctionhouse.model.CollectibleAuction;
import com.auctionhouse.model.JewelryAuction;
import com.auctionhouse.model.User;
import com.auctionhouse.model.WatchAuction;
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
                                // Car fields
                                @RequestParam(required = false) String carMake,
                                @RequestParam(required = false) String carModel,
                                @RequestParam(required = false, defaultValue = "0") int carYear,
                                @RequestParam(required = false, defaultValue = "0") int carMileage,
                                @RequestParam(required = false) String carCondition,
                                @RequestParam(required = false) String carColor,
                                // Watch fields
                                @RequestParam(required = false) String watchBrand,
                                @RequestParam(required = false) String watchModelName,
                                @RequestParam(required = false) String watchMovement,
                                @RequestParam(required = false) String watchCaseMaterial,
                                @RequestParam(required = false) String watchReference,
                                // Jewelry fields
                                @RequestParam(required = false) String jewelryMetal,
                                @RequestParam(required = false) String jewelryGemstone,
                                @RequestParam(required = false, defaultValue = "0") double jewelryCarat,
                                @RequestParam(required = false) String jewelryDesigner,
                                @RequestParam(required = false, defaultValue = "false") boolean jewelryCertified,
                                // Art fields
                                @RequestParam(required = false) String artArtist,
                                @RequestParam(required = false) String artMedium,
                                @RequestParam(required = false, defaultValue = "0") int artYearCreated,
                                @RequestParam(required = false) String artDimensions,
                                @RequestParam(required = false, defaultValue = "false") boolean artAuthenticated,
                                // Collectible fields
                                @RequestParam(required = false) String colSubcategory,
                                @RequestParam(required = false) String colEra,
                                @RequestParam(required = false) String colCondition,
                                @RequestParam(required = false) String colRarity,
                                @RequestParam(required = false) String colProvenance,
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

            // Instantiate the correct subclass based on category
            Auction auction;
            switch (cat) {
                case CARS: {
                    CarAuction ca = new CarAuction();
                    ca.setMake(safeStr(carMake, "Unknown"));
                    ca.setModel(safeStr(carModel, "Unknown"));
                    ca.setYear(carYear > 0 ? carYear : 2026);
                    ca.setMileage(carMileage);
                    ca.setCondition(safeStr(carCondition, "As described"));
                    ca.setColor(safeStr(carColor, "Not specified"));
                    auction = ca;
                    break;
                }
                case WATCHES: {
                    WatchAuction wa = new WatchAuction();
                    wa.setBrand(safeStr(watchBrand, "Unknown"));
                    wa.setModelName(safeStr(watchModelName, "Unknown"));
                    wa.setMovement(safeStr(watchMovement, "Not specified"));
                    wa.setCaseMaterial(safeStr(watchCaseMaterial, "Not specified"));
                    wa.setReferenceNumber(safeStr(watchReference, "N/A"));
                    auction = wa;
                    break;
                }
                case JEWELRY: {
                    JewelryAuction ja = new JewelryAuction();
                    ja.setMetalType(safeStr(jewelryMetal, "Not specified"));
                    ja.setGemstone(safeStr(jewelryGemstone, "Not specified"));
                    ja.setCarat(jewelryCarat);
                    ja.setDesigner(safeStr(jewelryDesigner, "Unknown"));
                    ja.setCertified(jewelryCertified);
                    auction = ja;
                    break;
                }
                case ART: {
                    ArtAuction aa = new ArtAuction();
                    aa.setArtist(safeStr(artArtist, "Unknown"));
                    aa.setMedium(safeStr(artMedium, "Not specified"));
                    aa.setYearCreated(artYearCreated > 0 ? artYearCreated : 2026);
                    aa.setDimensions(safeStr(artDimensions, "Not specified"));
                    aa.setAuthenticated(artAuthenticated);
                    auction = aa;
                    break;
                }
                case COLLECTIBLES:
                default: {
                    CollectibleAuction ca = new CollectibleAuction();
                    ca.setSubcategory(safeStr(colSubcategory, cat.getDisplayName()));
                    ca.setEra(safeStr(colEra, "Modern"));
                    ca.setCondition(safeStr(colCondition, "As described"));
                    ca.setRarity(safeStr(colRarity, "User Listed"));
                    ca.setProvenance(safeStr(colProvenance, ""));
                    auction = ca;
                    break;
                }
            }

            // Set common fields
            auction.setTitle(title);
            auction.setDescription(description);
            auction.setStartingPrice(startingPrice);
            auction.setImageUrl(finalImageUrl);
            auction.setCategory(cat);
            auction.setEndTime(LocalDateTime.now().plusMinutes(durationMinutes));
            auction.setCreatedBy(creator);
            // Status is already PENDING_APPROVAL by default from Auction constructor

            auctionService.save(auction);
            redirectAttributes.addFlashAttribute("success", "Your auction has been submitted for review! You'll be notified once it's approved. 📋");
            return "redirect:/my-auctions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create auction: " + e.getMessage());
            return "redirect:/auctions/create";
        }
    }

    private static String safeStr(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }
}
