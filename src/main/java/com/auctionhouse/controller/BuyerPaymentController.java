package com.auctionhouse.controller;

import com.auctionhouse.model.Conversation;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.User;
import com.auctionhouse.service.ChatService;
import com.auctionhouse.service.FileStorageService;
import com.auctionhouse.service.PaymentReleaseService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * BuyerPaymentController - handles buyer received-item details submission.
 */
@Controller
@RequestMapping("/buyer/payment")
public class BuyerPaymentController {

    private final PaymentReleaseService paymentReleaseService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ChatService chatService;

    @Autowired
    public BuyerPaymentController(PaymentReleaseService paymentReleaseService, UserService userService,
                                   FileStorageService fileStorageService, ChatService chatService) {
        this.paymentReleaseService = paymentReleaseService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.chatService = chatService;
    }

    /**
     * Show buyer details submission form.
     */
    @GetMapping("/submit/{auctionId}")
    public String showSubmitForm(@PathVariable Long auctionId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            User buyer = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            PaymentRelease pr = paymentReleaseService.findByAuctionId(auctionId)
                .orElseThrow(() -> new RuntimeException("Payment release not found"));

            // Security: Only buyer can access
            if (!pr.getBuyer().getId().equals(buyer.getId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to access this page.");
                return "redirect:/dashboard";
            }

            model.addAttribute("paymentRelease", pr);
            model.addAttribute("auction", pr.getAuction());

            // Fetch conversation for chat panel
            Conversation conversation = chatService.getConversationByPaymentRelease(pr);
            model.addAttribute("conversation", conversation);
            
            return "buyer/submit-details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Submit buyer received-item details.
     */
    @PostMapping("/submit/{paymentReleaseId}")
    public String submitDetails(@PathVariable Long paymentReleaseId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam Boolean receivedConfirmation,
                                 @RequestParam(required = false) String receivedDate,
                                 @RequestParam(required = false) MultipartFile proofFile,
                                 @RequestParam(required = false) String note,
                                 RedirectAttributes redirectAttributes) {
        try {
            User buyer = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Handle file upload
            String proofPath = null;
            if (proofFile != null && !proofFile.isEmpty()) {
                String filename = fileStorageService.storeProofFile(proofFile, "buyer_" + paymentReleaseId);
                proofPath = "/uploads/proof/" + filename;
            }

            // Parse received date
            LocalDateTime receivedDateTime = null;
            if (receivedDate != null && !receivedDate.isEmpty()) {
                receivedDateTime = LocalDateTime.parse(receivedDate + "T00:00:00");
            }

            paymentReleaseService.submitBuyerDetails(
                paymentReleaseId, buyer, receivedConfirmation, receivedDateTime, proofPath, note
            );

            redirectAttributes.addFlashAttribute("success", "Received-item details submitted successfully!");
            return "redirect:/dashboard";
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to perform this action.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit details: " + e.getMessage());
            return "redirect:/buyer/payment/submit/" + paymentReleaseId;
        }
    }
}
