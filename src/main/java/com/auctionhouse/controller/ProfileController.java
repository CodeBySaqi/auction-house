package com.auctionhouse.controller;

import com.auctionhouse.model.Auction;
import com.auctionhouse.model.AuctionStatus;
import com.auctionhouse.model.Bid;
import com.auctionhouse.model.Notification;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.User;
import com.auctionhouse.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProfileController - handles user dashboard, profile editing, and notifications.
 */
@Controller
public class ProfileController {

    private final UserService userService;
    private final BidService bidService;
    private final AuctionService auctionService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final PaymentReleaseService paymentReleaseService;

    @Autowired
    public ProfileController(UserService userService, BidService bidService,
                            AuctionService auctionService, NotificationService notificationService,
                            FileStorageService fileStorageService, PaymentReleaseService paymentReleaseService) {
        this.userService = userService;
        this.bidService = bidService;
        this.auctionService = auctionService;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.paymentReleaseService = paymentReleaseService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<Bid> userBids = bidService.getBidsByUser(user.getId());
        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        long unreadCount = notificationService.getUnreadCount(user.getId());

        // Get auctions user has won
        List<Auction> wonAuctions = auctionService.getAllAuctions().stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED &&
                        a.getHighestBidder() != null &&
                        a.getHighestBidder().getId().equals(user.getId()))
                .collect(Collectors.toList());

        // Get active bids (bids on active auctions)
        List<Bid> activeBids = userBids.stream()
                .filter(b -> b.getAuction().getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        // Get payment releases where user is seller (needs to submit delivery details)
        List<PaymentRelease> sellerPaymentReleases = paymentReleaseService.getBySeller(user);

        // Get payment releases where user is buyer (needs to confirm receipt)
        List<PaymentRelease> buyerPaymentReleases = paymentReleaseService.getByBuyer(user);

        model.addAttribute("user", user);
        model.addAttribute("totalBids", userBids.size());
        model.addAttribute("activeBids", activeBids.size());
        model.addAttribute("wonAuctions", wonAuctions);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("sellerPaymentReleases", sellerPaymentReleases);
        model.addAttribute("buyerPaymentReleases", buyerPaymentReleases);
        return "profile/dashboard";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/profile/upload-pic")
    public String uploadProfilePic(@RequestParam("profilePic") MultipartFile file,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload.");
                return "redirect:/profile/edit";
            }

            String filename = fileStorageService.storeProfilePic(file);
            userService.updateProfilePic(userDetails.getUsername(), filename);
            redirectAttributes.addFlashAttribute("success", "Profile picture updated successfully!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload: " + e.getMessage());
        }

        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String username,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String currentPassword,
                                 @RequestParam(required = false) String newPassword,
                                 @RequestParam(required = false) String confirmNewPassword,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser(userDetails.getUsername());
            userService.updateUserProfile(user.getId(), username, email,
                    currentPassword, newPassword, confirmNewPassword);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
        }
        return "redirect:/profile/edit";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<Notification> notifications = notificationService.getUserNotifications(user.getId());
        model.addAttribute("notifications", notifications);
        return "profile/notifications";
    }

    @PostMapping("/notifications/mark-read")
    public String markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        notificationService.markAllAsRead(user.getId());
        return "redirect:/dashboard";
    }

    @GetMapping("/my-bids")
    public String myBids(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<Bid> bids = bidService.getBidsByUser(user.getId());
        model.addAttribute("bids", bids);
        model.addAttribute("user", user);
        return "profile/mybids";
    }

    @GetMapping("/my-wins")
    public String myWins(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<Auction> wonAuctions = auctionService.getAllAuctions().stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED &&
                        a.getHighestBidder() != null &&
                        a.getHighestBidder().getId().equals(user.getId()))
                .collect(Collectors.toList());
        model.addAttribute("wonAuctions", wonAuctions);
        model.addAttribute("user", user);
        return "profile/mywins";
    }

    /**
     * Add $50,000 demo money to user's wallet.
     */
    @PostMapping("/profile/add-money")
    public String addDemoMoney(@AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        user.setWalletBalance(user.getWalletBalance() + 50000.0);
        userService.updateProfile(user);
        redirectAttributes.addFlashAttribute("success", "💰 $50,000 added to your wallet! Go bid on something awesome.");
        return "redirect:/dashboard";
    }

    /**
     * My Auctions — auctions created by the user.
     */
    @GetMapping("/my-auctions")
    public String myAuctions(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        List<Auction> userAuctions = auctionService.getAuctionsByCreator(user);
        model.addAttribute("userAuctions", userAuctions);
        model.addAttribute("user", user);
        return "profile/myauctions";
    }
}
