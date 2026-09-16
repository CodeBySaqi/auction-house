package com.auctionhouse.controller;

import com.auctionhouse.model.Conversation;
import com.auctionhouse.model.PaymentRelease;
import com.auctionhouse.model.PlatformCommission;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SellerPaymentController - handles seller delivery details submission.
 */
@Controller
@RequestMapping("/seller/payment")
public class SellerPaymentController {

    private final PaymentReleaseService paymentReleaseService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ChatService chatService;

    @Autowired
    public SellerPaymentController(PaymentReleaseService paymentReleaseService, UserService userService,
                                    FileStorageService fileStorageService, ChatService chatService) {
        this.paymentReleaseService = paymentReleaseService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.chatService = chatService;
    }

    /**
     * Show seller details submission form.
     */
    @GetMapping("/submit/{auctionId}")
    public String showSubmitForm(@PathVariable Long auctionId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            PaymentRelease pr = paymentReleaseService.findByAuctionId(auctionId)
                .orElseThrow(() -> new RuntimeException("Payment release not found"));

            // Security: Only seller can access
            if (!pr.getSeller().getId().equals(seller.getId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to access this page.");
                return "redirect:/dashboard";
            }

            model.addAttribute("paymentRelease", pr);
            model.addAttribute("auction", pr.getAuction());

            // Commission breakdown (server-side)
            BigDecimal[] breakdown = PlatformCommission.calculate(pr.getWinningAmount());
            model.addAttribute("commissionAmount", breakdown[0]);
            model.addAttribute("sellerPayoutAmount", breakdown[1]);

            // Fetch conversation for chat panel
            Conversation conversation = chatService.getConversationByPaymentRelease(pr);
            model.addAttribute("conversation", conversation);

            return "seller/submit-details";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    /**
     * Submit seller delivery details.
     */
    @PostMapping("/submit/{paymentReleaseId}")
    public String submitDetails(@PathVariable Long paymentReleaseId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String shippingMethod,
                                 @RequestParam(required = false) String trackingNumber,
                                 @RequestParam(required = false) String courierName,
                                 @RequestParam(required = false) String shipmentDate,
                                 @RequestParam(required = false) MultipartFile proofFile,
                                 @RequestParam(required = false) String note,
                                 RedirectAttributes redirectAttributes) {
        try {
            User seller = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Handle file upload
            String proofPath = null;
            if (proofFile != null && !proofFile.isEmpty()) {
                String filename = fileStorageService.storeProofFile(proofFile, "seller_" + paymentReleaseId);
                proofPath = "/uploads/proof/" + filename;
            }

            // Parse shipment date
            LocalDateTime shipmentDateTime = null;
            if (shipmentDate != null && !shipmentDate.isEmpty()) {
                shipmentDateTime = LocalDateTime.parse(shipmentDate + "T00:00:00");
            }

            paymentReleaseService.submitSellerDetails(
                paymentReleaseId, seller, shippingMethod, trackingNumber,
                courierName, shipmentDateTime, proofPath, note
            );

            redirectAttributes.addFlashAttribute("success", "Delivery details submitted successfully!");
            return "redirect:/dashboard";
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to perform this action.");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit details: " + e.getMessage());
            return "redirect:/seller/payment/submit/" + paymentReleaseId;
        }
    }
}
