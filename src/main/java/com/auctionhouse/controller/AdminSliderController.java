package com.auctionhouse.controller;

import com.auctionhouse.model.Slide;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.SlideRepository;
import com.auctionhouse.service.FileStorageService;
import com.auctionhouse.service.NotificationService;
import com.auctionhouse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.auctionhouse.service.AuditService;
import java.security.Principal;
import java.util.List;

/**
 * Admin controller for managing homepage hero slider slides.
 */
@Controller
@RequestMapping("/admin/slides")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminSliderController {

    private final SlideRepository slideRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    @Autowired
    public AdminSliderController(SlideRepository slideRepository,
                                  UserService userService,
                                  NotificationService notificationService,
                                  FileStorageService fileStorageService,
                                      AuditService auditService) {
        this.slideRepository = slideRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
    }

    @GetMapping({"", "/"})
    public String slidesPage(Model model, Principal principal) {
        User admin = userService.findByUsername(principal.getName()).orElse(null);
        if (admin == null) return "redirect:/login";

        List<Slide> slides = slideRepository.findAllByOrderBySortOrderAsc();
        model.addAttribute("admin", admin);
        model.addAttribute("slides", slides);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        return "admin-slides";
    }

    @PostMapping("/add")
    public String addSlide(@RequestParam String title,
                           @RequestParam String subtitle,
                           @RequestParam(required = false) String buttonText,
                           @RequestParam(required = false) String buttonUrl,
                           @RequestParam(required = false, defaultValue = "#3b82f6") String buttonColor,
                           @RequestParam(required = false, defaultValue = "#ffffff") String backgroundColor,
                           @RequestParam(required = false) String backgroundGradient,
                           @RequestParam(required = false, defaultValue = "#1f2937") String textColor,
                           @RequestParam(required = false, defaultValue = "#4b5563") String subtitleColor,
                           @RequestParam(required = false) String imageUrl,
                           @RequestParam(required = false) MultipartFile slideImage,
                           @RequestParam(required = false, defaultValue = "rounded-[40%_60%_70%_30%/40%_50%_60%_50%]") String imageShape,
                           @RequestParam(required = false, defaultValue = "0") int sortOrder,
                           @RequestParam(required = false, defaultValue = "true") boolean active,
                           Principal principal,
                           RedirectAttributes ra) {
        if (title == null || title.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Title is required.");
            return "redirect:/admin/slides";
        }

        // Resolve image: upload takes priority over URL
        String finalImageUrl = (imageUrl != null && !imageUrl.trim().isEmpty()) ? imageUrl.trim() : "";
        if (slideImage != null && !slideImage.isEmpty()) {
            try {
                String filename = fileStorageService.storeAuctionImage(slideImage);
                finalImageUrl = "/uploads/auctions/" + filename;
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
                return "redirect:/admin/slides";
            }
        }

        Slide slide = new Slide();
        slide.setTitle(title.trim());
        slide.setSubtitle(subtitle != null ? subtitle.trim() : "");
        slide.setButtonText(buttonText != null ? buttonText.trim() : "Learn More");
        slide.setButtonUrl(buttonUrl != null ? buttonUrl.trim() : "/auctions");
        slide.setButtonColor(buttonColor);
        slide.setBackgroundColor(backgroundColor);
        slide.setBackgroundGradient(backgroundGradient != null && !backgroundGradient.trim().isEmpty() ? backgroundGradient.trim() : null);
        slide.setTextColor(textColor);
        slide.setSubtitleColor(subtitleColor);
        slide.setImageUrl(finalImageUrl);
        slide.setImageShape(imageShape);
        slide.setSortOrder(sortOrder);
        slide.setActive(active);
        slideRepository.save(slide);
        auditService.log(userService.findByUsername(principal.getName()).orElse(null), "SLIDE_CREATED", null, "Slide #" + slide.getId(),
                "Created hero slide '" + slide.getTitle() + "' (sortOrder " + slide.getSortOrder() + ")");

        ra.addFlashAttribute("success", "Slide added successfully!");
        return "redirect:/admin/slides";
    }

    @PostMapping("/update/{id}")
    public String updateSlide(@PathVariable Long id,
                              @RequestParam String title,
                              @RequestParam String subtitle,
                              @RequestParam(required = false) String buttonText,
                              @RequestParam(required = false) String buttonUrl,
                              @RequestParam(required = false, defaultValue = "#3b82f6") String buttonColor,
                              @RequestParam(required = false, defaultValue = "#ffffff") String backgroundColor,
                              @RequestParam(required = false) String backgroundGradient,
                              @RequestParam(required = false, defaultValue = "#1f2937") String textColor,
                              @RequestParam(required = false, defaultValue = "#4b5563") String subtitleColor,
                              @RequestParam(required = false) String imageUrl,
                              @RequestParam(required = false) MultipartFile slideImage,
                              @RequestParam(required = false, defaultValue = "rounded-[40%_60%_70%_30%/40%_50%_60%_50%]") String imageShape,
                              @RequestParam(required = false, defaultValue = "0") int sortOrder,
                              @RequestParam(required = false, defaultValue = "true") boolean active,
                              Principal principal,
                           RedirectAttributes ra) {
        Slide slide = slideRepository.findById(id).orElse(null);
        if (slide == null) {
            ra.addFlashAttribute("error", "Slide not found.");
            return "redirect:/admin/slides";
        }

        slide.setTitle(title.trim());
        slide.setSubtitle(subtitle != null ? subtitle.trim() : "");
        slide.setButtonText(buttonText != null ? buttonText.trim() : "Learn More");
        slide.setButtonUrl(buttonUrl != null ? buttonUrl.trim() : "/auctions");
        slide.setButtonColor(buttonColor);
        slide.setBackgroundColor(backgroundColor);
        slide.setBackgroundGradient(backgroundGradient != null && !backgroundGradient.trim().isEmpty() ? backgroundGradient.trim() : null);
        slide.setTextColor(textColor);
        slide.setSubtitleColor(subtitleColor);
        slide.setImageShape(imageShape);
        slide.setSortOrder(sortOrder);
        slide.setActive(active);

        // Image: upload takes priority, then URL, then keep existing
        if (slideImage != null && !slideImage.isEmpty()) {
            try {
                String filename = fileStorageService.storeAuctionImage(slideImage);
                slide.setImageUrl("/uploads/auctions/" + filename);
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
                return "redirect:/admin/slides";
            }
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            slide.setImageUrl(imageUrl.trim());
        }
        // else: keep existing imageUrl unchanged

        slideRepository.save(slide);
        auditService.log(userService.findByUsername(principal.getName()).orElse(null), "SLIDE_UPDATED", null, "Slide #" + slide.getId(),
                "Edited hero slide '" + slide.getTitle() + "'");

        ra.addFlashAttribute("success", "Slide updated!");
        return "redirect:/admin/slides";
    }

    @PostMapping("/delete/{id}")
    public String deleteSlide(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Slide slide = slideRepository.findById(id).orElse(null);
        String slideTitle = slide != null ? slide.getTitle() : ("#" + id);
        slideRepository.deleteById(id);
        auditService.log(userService.findByUsername(principal.getName()).orElse(null), "SLIDE_DELETED", null, "Slide #" + id,
                "Deleted hero slide '" + slideTitle + "' from the homepage carousel");
        ra.addFlashAttribute("success", "Slide deleted.");
        return "redirect:/admin/slides";
    }

    @PostMapping("/toggle/{id}")
    public String toggleSlide(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Slide slide = slideRepository.findById(id).orElse(null);
        if (slide != null) {
            slide.setActive(!slide.isActive());
            slideRepository.save(slide);
            auditService.log(userService.findByUsername(principal.getName()).orElse(null), slide.isActive() ? "SLIDE_ACTIVATED" : "SLIDE_DEACTIVATED",
                    null, "Slide #" + id, (slide.isActive() ? "Activated" : "Deactivated") + " hero slide '" + slide.getTitle() + "'");
            ra.addFlashAttribute("success", "Slide " + (slide.isActive() ? "activated" : "deactivated") + ".");
        }
        return "redirect:/admin/slides";
    }
}
