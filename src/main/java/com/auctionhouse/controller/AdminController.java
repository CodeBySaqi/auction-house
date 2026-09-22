package com.auctionhouse.controller;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.BidRepository;
import com.auctionhouse.repository.BroadcastRepository;
import com.auctionhouse.repository.UserRepository;
import com.auctionhouse.service.AuctionService;
import com.auctionhouse.service.ChatService;
import com.auctionhouse.service.NotificationService;
import com.auctionhouse.service.PaymentReleaseService;
import com.auctionhouse.service.FileStorageService;
import com.auctionhouse.service.UserService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin console for the auction platform. Only reachable by ROLE_ADMIN.
 *
 * The console is one template with tabs; every tab is populated from real
 * repository data, so the numbers on screen always agree with the tables below.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d");
    private static final int ACTIVITY_DAYS = 14;

    private final UserService userService;
    private final AuctionService auctionService;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final PaymentReleaseService paymentReleaseService;
    private final FileStorageService fileStorageService;
    private final ChatService chatService;
    private final BroadcastRepository broadcastRepository;

    @Autowired
    public AdminController(UserService userService,
                           AuctionService auctionService,
                           BidRepository bidRepository,
                           UserRepository userRepository,
                           NotificationService notificationService,
                           PasswordEncoder passwordEncoder,
                           PaymentReleaseService paymentReleaseService,
                           FileStorageService fileStorageService,
                           ChatService chatService,
                           BroadcastRepository broadcastRepository) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.paymentReleaseService = paymentReleaseService;
        this.fileStorageService = fileStorageService;
        this.chatService = chatService;
        this.broadcastRepository = broadcastRepository;
    }

    /* ==================== NEW ADMIN CONSOLE ==================== */

    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboardRedirect() {
        return "redirect:/admin/console";
    }

    @GetMapping("/console")
    public String adminConsole(Model model, Principal principal) {
        User admin = requireAdmin(principal);
        List<User> allUsers = userService.findAllUsers();
        List<Auction> allAuctions = auctionService.getAllAuctions();
        LocalDateTime now = LocalDateTime.now();

        model.addAttribute("admin", admin);

        // KPI metrics
        model.addAttribute("totalAuctions", allAuctions.size());
        model.addAttribute("totalUsers", allUsers.size());
        
        long newUsersLast24h = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(now.minusHours(24)))
                .count();
        model.addAttribute("newUsersLast24h", newUsersLast24h);
        
        int totalBids = allAuctions.stream().mapToInt(Auction::getBidCount).sum();
        model.addAttribute("totalBids", totalBids);
        
        double totalRevenue = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED)
                .mapToDouble(Auction::getCurrentHighestBid).sum();
        model.addAttribute("totalRevenue", totalRevenue);
        
        long endingSoon = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE
                        && a.getEndTime() != null
                        && !a.getEndTime().isAfter(now.plusHours(24)))
                .count();
        model.addAttribute("endingSoon", endingSoon);
        
        long newUsersToday = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(now.toLocalDate().atStartOfDay()))
                .count();
        model.addAttribute("newUsersToday", newUsersToday);

        // Activity chart data (14 days)
        List<Bid> recentBidsAll = bidRepository.findBidsSince(now.minusDays(ACTIVITY_DAYS).toLocalDate().atStartOfDay());
        Map<LocalDate, Long> bidsByDay = recentBidsAll.stream()
                .filter(b -> b.getTimestamp() != null)
                .collect(Collectors.groupingBy(b -> b.getTimestamp().toLocalDate(), Collectors.counting()));
        
        Map<LocalDate, Long> signupsByDay = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()));

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> activityData = new ArrayList<>();
        long maxBids = 1L;
        long maxSignups = 1L;
        
        for (int i = ACTIVITY_DAYS - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            long bids = bidsByDay.getOrDefault(d, 0L);
            long signups = signupsByDay.getOrDefault(d, 0L);
            maxBids = Math.max(maxBids, bids);
            maxSignups = Math.max(maxSignups, signups);
            
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("label", d.format(DAY_LABEL));
            day.put("bids", bids);
            day.put("signups", signups);
            activityData.add(day);
        }
        
        for (Map<String, Object> day : activityData) {
            long bids = ((Number) day.get("bids")).longValue();
            long signups = ((Number) day.get("signups")).longValue();
            day.put("bidPercent", Math.round(bids * 100.0 / maxBids));
            day.put("signupPercent", Math.round(signups * 100.0 / maxSignups));
        }
        model.addAttribute("activityData", activityData);

        // Category stats with colors
        List<Map<String, Object>> categoryStats = new ArrayList<>();
        String[] colors = {"bg-g-blue", "bg-g-green", "bg-purple-500", "bg-amber-500", "bg-g-red"};
        int colorIdx = 0;
        for (AuctionCategory c : AuctionCategory.values()) {
            long count = allAuctions.stream().filter(a -> a.getCategory() == c).count();
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("name", c.getDisplayName());
            cat.put("count", count);
            cat.put("percent", allAuctions.isEmpty() ? 0 : Math.round(count * 100.0 / allAuctions.size()));
            cat.put("color", colors[colorIdx % colors.length]);
            categoryStats.add(cat);
            colorIdx++;
        }
        categoryStats.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        model.addAttribute("categoryStats", categoryStats);

        // Recent bids with formatting
        List<Bid> recentBidsRaw = bidRepository.findRecentBids(PageRequest.of(0, 5));
        List<Map<String, Object>> recentBids = new ArrayList<>();
        String[] bidColors = {"bg-g-blue-light text-g-blue", "bg-green-50 text-g-green", "bg-purple-50 text-purple-600", 
                              "bg-amber-50 text-amber-600", "bg-red-50 text-g-red"};
        int bidColorIdx = 0;
        for (Bid bid : recentBidsRaw) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("username", bid.getBidder().getUsername());
            b.put("initials", bid.getBidder().getUsername().substring(0, 2).toUpperCase());
            b.put("auctionTitle", bid.getAuction().getTitle());
            b.put("amount", bid.getAmount());
            b.put("timeAgo", getTimeAgo(bid.getTimestamp()));
            b.put("colorClass", bidColors[bidColorIdx % bidColors.length]);
            recentBids.add(b);
            bidColorIdx++;
        }
        model.addAttribute("recentBids", recentBids);

        // Top bidders with formatting
        Map<Long, Long> bidsPerUser = new HashMap<>();
        Map<Long, Long> winsPerUser = new HashMap<>();
        for (Object[] row : bidRepository.countBidsGroupedByBidder()) {
            if (row[0] != null) bidsPerUser.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        for (Auction a : allAuctions) {
            if (a.getStatus() == AuctionStatus.CLOSED && a.getHighestBidder() != null) {
                winsPerUser.merge(a.getHighestBidder().getId(), 1L, Long::sum);
            }
        }
        
        List<Map<String, Object>> topBidders = allUsers.stream()
                .sorted(Comparator.comparingLong((User u) -> bidsPerUser.getOrDefault(u.getId(), 0L)).reversed())
                .limit(5)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("username", u.getUsername());
                    m.put("initials", u.getUsername().substring(0, 2).toUpperCase());
                    m.put("bidCount", bidsPerUser.getOrDefault(u.getId(), 0L));
                    m.put("winCount", winsPerUser.getOrDefault(u.getId(), 0L));
                    m.put("totalSpent", allAuctions.stream()
                            .filter(a -> a.getStatus() == AuctionStatus.CLOSED 
                                    && a.getHighestBidder() != null 
                                    && a.getHighestBidder().getId().equals(u.getId()))
                            .mapToDouble(Auction::getCurrentHighestBid).sum());
                    String[] userColors = {"bg-gradient-to-br from-blue-400 to-blue-600", 
                                          "bg-gradient-to-br from-green-400 to-green-600",
                                          "bg-gradient-to-br from-purple-400 to-purple-600",
                                          "bg-gradient-to-br from-red-400 to-red-600",
                                          "bg-gradient-to-br from-amber-400 to-amber-600"};
                    m.put("colorClass", userColors[allUsers.indexOf(u) % userColors.length]);
                    return m;
                })
                .collect(Collectors.toList());
        model.addAttribute("topBidders", topBidders);

        // Ending soon auctions
        List<Auction> endingSoonAuctions = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE && a.getEndTime() != null)
                .sorted(Comparator.comparing(Auction::getEndTime))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("endingSoonAuctions", endingSoonAuctions);

        // Notification count
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));

        // Pending approvals count
        model.addAttribute("pendingCount", auctionService.getPendingAuctions().size());

        // Pending payment releases count
        model.addAttribute("pendingPaymentCount", paymentReleaseService.countPendingReview());

        return "admin-console";
    }

    private String getTimeAgo(LocalDateTime timestamp) {
        if (timestamp == null) return "Unknown";
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(timestamp, now);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        return days + "d ago";
    }

    /* ==================== SEARCH ==================== */

    @GetMapping("/search")
    public String adminSearch(@RequestParam String q, Principal principal) {
        requireAdmin(principal);
        if (q == null || q.trim().isEmpty()) {
            return "redirect:/admin/console";
        }
        String query = q.trim().toLowerCase();

        // Check if search matches a user (username or email)
        List<User> allUsers = userService.findAllUsers();
        boolean userMatch = allUsers.stream().anyMatch(u ->
                u.getUsername().toLowerCase().contains(query) ||
                (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)));

        // Check if search matches an auction (title)
        List<Auction> allAuctions = auctionService.getAllAuctions();
        boolean auctionMatch = allAuctions.stream().anyMatch(a ->
                a.getTitle().toLowerCase().contains(query));

        // If only user matches, go to users page
        if (userMatch && !auctionMatch) {
            return "redirect:/admin/users?search=" + java.net.URLEncoder.encode(q.trim(), java.nio.charset.StandardCharsets.UTF_8);
        }
        // Default: go to auctions page (covers auction matches and no matches)
        return "redirect:/admin/auctions?search=" + java.net.URLEncoder.encode(q.trim(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /* ==================== AUCTIONS ==================== */

    @GetMapping("/auctions")
    public String manageAuctions(Model model, Principal principal,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        addSidebarAttributes(model);

        List<Auction> auctions = auctionService.getAllAuctions();

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                AuctionStatus s = AuctionStatus.valueOf(status.toUpperCase());
                auctions = auctions.stream().filter(a -> a.getStatus() == s).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) { /* unknown status, show all */ }
        }
        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("ALL")) {
            try {
                AuctionCategory c = AuctionCategory.valueOf(category.toUpperCase());
                auctions = auctions.stream().filter(a -> a.getCategory() == c).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) { /* unknown category, show all */ }
        }
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            auctions = auctions.stream()
                    .filter(a -> (a.getTitle() != null && a.getTitle().toLowerCase().contains(q))
                            || (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }
        auctions.sort(Comparator.comparing(Auction::getCreatedAt).reversed());

        model.addAttribute("auctions", auctions);
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("currentCategory", category != null ? category : "ALL");
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("allCategories", AuctionCategory.values());
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        addCounts(model, auctionService.getAllAuctions());
        addFormatters(model);
        return "admin-auctions";
    }

    @PostMapping("/auctions/toggle/{id}")
    public String toggleAuctionStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Auction auction = auctionService.findById(id).orElseThrow(() -> new IllegalArgumentException("Auction not found"));
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                auctionService.closeAuctionManually(auction);
                ra.addFlashAttribute("successMessage", "Closed \"" + auction.getTitle() + "\".");
            } else {
                // BUG 7: Check for existing PaymentRelease before reopening
                Optional<PaymentRelease> existingPR = paymentReleaseService.findByAuctionId(auction.getId());
                if (existingPR.isPresent()) {
                    PaymentRelease pr = existingPR.get();
                    if (pr.isPaymentReleased()) {
                        ra.addFlashAttribute("errorMessage", "Cannot reopen \"" + auction.getTitle() + "\" — payment has already been released to the seller.");
                    } else {
                        ra.addFlashAttribute("errorMessage", "Cannot reopen \"" + auction.getTitle() + "\" — payment release record exists. Please resolve payment release first.");
                    }
                    return "redirect:/admin/auctions";
                }
                // Refund highest bidder and reset bid state before reopening
                auctionService.clearAuctionBids(auction);
                auction.setStatus(AuctionStatus.ACTIVE);
                auction.setEndTime(LocalDateTime.now().plusDays(7));
                auctionService.save(auction);
                ra.addFlashAttribute("successMessage", "Reopened \"" + auction.getTitle() + "\" and set it to run for 7 days.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Could not update the auction: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/extend/{id}")
    public String extendAuction(@PathVariable Long id,
                                @RequestParam(defaultValue = "1") int hours,
                                RedirectAttributes ra) {
        try {
            Auction auction = auctionService.findById(id).orElseThrow(() -> new IllegalArgumentException("Auction not found"));
            LocalDateTime base = (auction.getEndTime() != null && auction.getEndTime().isAfter(LocalDateTime.now()))
                    ? auction.getEndTime() : LocalDateTime.now();
            auction.setEndTime(base.plusHours(hours));
            auction.setStatus(AuctionStatus.ACTIVE);
            auctionService.save(auction);
            ra.addFlashAttribute("successMessage", "Extended \"" + auction.getTitle() + "\" by " + hours + "h.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Could not extend the auction: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/cancel/{id}")
    public String cancelAuction(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Auction auction = auctionService.findById(id).orElseThrow(() -> new IllegalArgumentException("Auction not found"));
            auctionService.cancelAuction(auction);
            ra.addFlashAttribute("successMessage", "Cancelled \"" + auction.getTitle() + "\" and refunded the highest bidder.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Could not cancel the auction: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    @PostMapping("/auctions/delete/{id}")
    public String deleteAuction(@PathVariable Long id, RedirectAttributes ra) {
        try {
            auctionService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Auction deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to delete auction: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    /** Bulk action over a set of auction ids. */
    @PostMapping("/auctions/bulk")
    public String bulkAuctions(@RequestParam(defaultValue = "close") String action,
                               @RequestParam(name = "ids", required = false) List<Long> ids,
                               RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Select at least one auction first.");
            return "redirect:/admin/auctions";
        }
        int done = 0;
        try {
            for (Long id : ids) {
                Optional<Auction> opt = auctionService.findById(id);
                if (!opt.isPresent()) continue;
                Auction a = opt.get();
                switch (action) {
                    case "close":
                        auctionService.closeAuctionManually(a);
                        break;
                    case "reopen":
                        // Skip if already ACTIVE
                        if (a.getStatus() == AuctionStatus.ACTIVE) {
                            ra.addFlashAttribute("errorMessage", "Auction \"" + a.getTitle() + "\" is already active.");
                            continue;
                        }
                        // Check for existing PaymentRelease - block if found (safer option)
                        Optional<PaymentRelease> existingPR = paymentReleaseService.findByAuctionId(a.getId());
                        if (existingPR.isPresent()) {
                            PaymentRelease pr = existingPR.get();
                            if (pr.isPaymentReleased()) {
                                ra.addFlashAttribute("errorMessage", "Cannot reopen \"" + a.getTitle() + "\" — payment has already been released to the seller.");
                                continue;
                            } else {
                                ra.addFlashAttribute("errorMessage", "Cannot reopen \"" + a.getTitle() + "\" — payment release record exists. Please resolve payment release first.");
                                continue;
                            }
                        }
                        // Refund highest bidder and reset bid state (without "cancelled" notification)
                        auctionService.clearAuctionBids(a);
                        a.setStatus(AuctionStatus.ACTIVE);
                        a.setEndTime(LocalDateTime.now().plusDays(7));
                        auctionService.save(a);
                        break;
                    case "cancel":
                        auctionService.cancelAuction(a);
                        break;
                    case "delete":
                        auctionService.deleteById(a.getId());
                        break;
                    default:
                        break;
                }
                done++;
            }
            ra.addFlashAttribute("successMessage", done + " auction" + (done == 1 ? "" : "s") + " updated (" + action + ").");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Bulk action failed after " + done + " items: " + e.getMessage());
        }
        return "redirect:/admin/auctions";
    }

    /* ==================== USERS ==================== */

    @GetMapping("/users")
    public String manageUsers(Model model, Principal principal,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String role) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        addSidebarAttributes(model);

        List<User> users = userService.findAllUsers();
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }
        if (role != null && !role.isEmpty() && !role.equalsIgnoreCase("ALL")) {
            if ("INACTIVE".equalsIgnoreCase(role)) {
                users = users.stream().filter(u -> !u.isActive()).collect(Collectors.toList());
            } else {
                users = users.stream().filter(u -> role.equalsIgnoreCase(u.getRole())).collect(Collectors.toList());
            }
        }
        users.sort(Comparator.comparing(User::getCreatedAt).reversed());

        Map<Long, Long> bidsPerUser = new HashMap<>();
        for (Object[] row : bidRepository.countBidsGroupedByBidder()) {
            if (row[0] != null) bidsPerUser.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        model.addAttribute("users", users);
        model.addAttribute("bidsPerUser", bidsPerUser);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("currentRole", role != null ? role : "ALL");
        model.addAttribute("adminCount", userRepository.countByRole("ROLE_ADMIN"));
        model.addAttribute("bannedCount", userRepository.countByActive(false));
        model.addAttribute("userCount", userRepository.countByRole("ROLE_USER"));
        model.addAttribute("walletFloat", userRepository.sumWalletBalance());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        addCounts(model, auctionService.getAllAuctions());
        addFormatters(model);
        return "admin-users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam String role,
                             RedirectAttributes ra) {
        try {
            if (userService.findByUsername(username).isPresent()) {
                ra.addFlashAttribute("errorMessage", "Username \"" + username + "\" is already taken.");
                return "redirect:/admin/users";
            }
            if (userService.findByEmail(email).isPresent()) {
                ra.addFlashAttribute("errorMessage", "Email \"" + email + "\" is already registered.");
                return "redirect:/admin/users";
            }
            // SECURITY: ADMIN cannot create SUPER_ADMIN accounts
            if ("ROLE_SUPER_ADMIN".equals(role)) {
                ra.addFlashAttribute("errorMessage", "Only Super Admins can create SUPER_ADMIN accounts.");
                return "redirect:/admin/users";
            }
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode(password));
            u.setRole(role);
            u.setWalletBalance(100000.0);
            userService.updateProfile(u);
            ra.addFlashAttribute("successMessage", "Created " + role.replace("ROLE_", "").toLowerCase() + " \"" + username + "\".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to create user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/role/{id}")
    public String changeUserRole(@PathVariable Long id, @RequestParam String role,
                                 Principal principal, RedirectAttributes ra) {
        try {
            User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user.getUsername().equals(principal.getName())) {
                ra.addFlashAttribute("errorMessage", "You cannot change your own role.");
                return "redirect:/admin/users";
            }
            // SECURITY: ADMIN cannot assign SUPER_ADMIN role
            if ("ROLE_SUPER_ADMIN".equals(role)) {
                ra.addFlashAttribute("errorMessage", "Only Super Admins can assign the SUPER_ADMIN role.");
                return "redirect:/admin/users";
            }
            // SECURITY: ADMIN cannot modify SUPER_ADMIN accounts
            if ("ROLE_SUPER_ADMIN".equals(user.getRole())) {
                ra.addFlashAttribute("errorMessage", "Cannot modify a Super Admin account.");
                return "redirect:/admin/users";
            }
            String oldRole = user.getRole().replace("ROLE_", "");
            user.setRole(role);
            userService.updateProfile(user);
            ra.addFlashAttribute("successMessage", user.getUsername() + " moved from " + oldRole + " to " + role.replace("ROLE_", "") + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to change role: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
            // SECURITY: Cannot deactivate SUPER_ADMIN accounts
            if ("ROLE_SUPER_ADMIN".equals(user.getRole())) {
                ra.addFlashAttribute("errorMessage", "Cannot deactivate a Super Admin account.");
                return "redirect:/admin/users";
            }
            if ("ROLE_ADMIN".equals(user.getRole())) {
                ra.addFlashAttribute("errorMessage", "Administrators cannot be deactivated from here.");
                return "redirect:/admin/users";
            }
            if (!user.isActive()) {
                user.setActive(true);
                ra.addFlashAttribute("successMessage", "Reactivated \"" + user.getUsername() + "\".");
            } else {
                user.setActive(false);
                ra.addFlashAttribute("successMessage", "Deactivated \"" + user.getUsername() + "\".");
            }
            userService.updateProfile(user);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/balance/{id}")
    public String adjustBalance(@PathVariable Long id, @RequestParam double amount, RedirectAttributes ra) {
        try {
            User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
            double newBalance = user.getWalletBalance() + amount;
            if (newBalance < 0) {
                ra.addFlashAttribute("errorMessage", "Cannot reduce balance below $0.00. Current: $" 
                        + String.format("%,.2f", user.getWalletBalance()) + ", attempted deduction: $" 
                        + String.format("%,.2f", Math.abs(amount)) + ".");
                return "redirect:/admin/users";
            }
            user.setWalletBalance(newBalance);
            userService.updateProfile(user);
            ra.addFlashAttribute("successMessage", "Adjusted " + user.getUsername() + " by "
                    + String.format("%,.2f", amount) + ". New balance $" + String.format("%,.2f", user.getWalletBalance()) + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update balance: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            User user = userService.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user.getUsername().equals(principal.getName())) {
                ra.addFlashAttribute("errorMessage", "You cannot delete your own account.");
                return "redirect:/admin/users";
            }
            userService.deleteById(id);
            ra.addFlashAttribute("successMessage", "Deleted \"" + user.getUsername() + "\".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** Bulk action over a set of user ids. */
    @PostMapping("/users/bulk")
    public String bulkUsers(@RequestParam(defaultValue = "ban") String action,
                            @RequestParam(name = "ids", required = false) List<Long> ids,
                            Principal principal, RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Select at least one account first.");
            return "redirect:/admin/users";
        }
        int done = 0;
        try {
            for (Long id : ids) {
                Optional<User> opt = userService.findById(id);
                if (!opt.isPresent()) continue;
                User u = opt.get();
                if (u.getUsername().equals(principal.getName())) continue;
                // SECURITY: Cannot perform bulk actions on SUPER_ADMIN accounts
                if ("ROLE_SUPER_ADMIN".equals(u.getRole())) continue;
                switch (action) {
                    case "ban":
                        if (!"ROLE_ADMIN".equals(u.getRole()) && !"ROLE_SUPER_ADMIN".equals(u.getRole())) {
                            u.setActive(false);
                            userService.updateProfile(u);
                        }
                        break;
                    case "unban":
                        if (!u.isActive()) {
                            u.setActive(true);
                            userService.updateProfile(u);
                        }
                        break;
                    case "delete":
                        if (!"ROLE_ADMIN".equals(u.getRole()) && !"ROLE_SUPER_ADMIN".equals(u.getRole())) {
                            userService.deleteById(u.getId());
                        }
                        break;
                    default:
                        break;
                }
                done++;
            }
            ra.addFlashAttribute("successMessage", done + " account" + (done == 1 ? "" : "s") + " updated (" + action + ").");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Bulk action failed after " + done + " items: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /* ==================== ANALYTICS ==================== */

    @GetMapping("/analytics")
    public String analytics(Model model, Principal principal) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        addSidebarAttributes(model);
        List<Auction> allAuctions = auctionService.getAllAuctions();
        List<User> allUsers = userService.findAllUsers();
        model.addAttribute("categoryBreakdown", breakdown(allAuctions));
        model.addAttribute("statusBreakdown", statusBreakdown(allAuctions));
        
        Map<String, Double> monthlyRevenueMap = monthlyRevenue(allAuctions);
        model.addAttribute("monthlyRevenue", monthlyRevenueMap);
        
        // Calculate max revenue for chart scaling
        double maxRevenue = monthlyRevenueMap.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        model.addAttribute("maxRevenue", maxRevenue);
        
        double totalRevenue = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED)
                .mapToDouble(Auction::getCurrentHighestBid).sum();
        model.addAttribute("totalRevenue", totalRevenue);
        
        int totalBids = allAuctions.stream().mapToInt(Auction::getBidCount).sum();
        model.addAttribute("totalBids", totalBids);
        model.addAttribute("avgBidsPerAuction", allAuctions.isEmpty() ? 0d : (double) totalBids / allAuctions.size());
        model.addAttribute("walletFloat", userRepository.sumWalletBalance());
        model.addAttribute("avgWallet", allUsers.isEmpty() ? 0d
                : allUsers.stream().mapToDouble(User::getWalletBalance).average().orElse(0d));

        // top sellers by listed value
        Map<String, Double> byCreator = allAuctions.stream()
                .filter(a -> a.getCreatedBy() != null)
                .collect(Collectors.groupingBy(a -> a.getCreatedBy().getUsername(),
                        Collectors.summingDouble(Auction::getDisplayPrice)));
        model.addAttribute("topSellers", byCreator.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, LinkedHashMap::new)));

        // Success rate: closed auctions with a winner / total closed auctions
        long closedAuctions = allAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.CLOSED).count();
        long closedWithWinner = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED && a.getHighestBidder() != null)
                .count();
        int successRate = closedAuctions == 0 ? 0 : (int) Math.round(closedWithWinner * 100.0 / closedAuctions);
        model.addAttribute("successRate", successRate);

        // Avg duration: average days from creation to end for closed auctions
        double avgDuration = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.CLOSED
                        && a.getCreatedAt() != null && a.getEndTime() != null)
                .mapToLong(a -> java.time.temporal.ChronoUnit.HOURS.between(a.getCreatedAt(), a.getEndTime()))
                .average()
                .orElse(0.0) / 24.0;
        model.addAttribute("avgDuration", String.format("%.1f", avgDuration));

        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        addCounts(model, allAuctions);
        addFormatters(model);
        return "admin-analytics";
    }

    @GetMapping("/export/auctions.csv")
    public ResponseEntity<byte[]> exportAuctions() {
        StringBuilder sb = new StringBuilder("id,title,category,status,starting_price,current_bid,bids,end_time,seller\n");
        for (Auction a : auctionService.getAllAuctions()) {
            sb.append(a.getId()).append(',')
              .append(csv(a.getTitle())).append(',')
              .append(a.getCategory() != null ? a.getCategory().name() : "").append(',')
              .append(a.getStatus() != null ? a.getStatus().name() : "").append(',')
              .append(fmtMoney(a.getStartingPrice())).append(',')
              .append(fmtMoney(a.getCurrentHighestBid())).append(',')
              .append(a.getBidCount()).append(',')
              .append(a.getEndTime() != null ? a.getEndTime().format(DATE_TIME) : "").append(',')
              .append(a.getCreatedBy() != null ? csv(a.getCreatedBy().getUsername()) : "")
              .append('\n');
        }
        return csvResponse("auctions.csv", sb.toString());
    }

    @GetMapping("/export/users.csv")
    public ResponseEntity<byte[]> exportUsers() {
        StringBuilder sb = new StringBuilder("id,username,email,role,wallet_balance,joined,bids\n");
        Map<Long, Long> bidsPerUser = new HashMap<>();
        for (Object[] row : bidRepository.countBidsGroupedByBidder()) {
            if (row[0] != null) bidsPerUser.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        for (User u : userService.findAllUsers()) {
            sb.append(u.getId()).append(',')
              .append(csv(u.getUsername())).append(',')
              .append(csv(u.getEmail())).append(',')
              .append(u.getRole()).append(',')
              .append(fmtMoney(u.getWalletBalance())).append(',')
              .append(u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_TIME) : "").append(',')
              .append(bidsPerUser.getOrDefault(u.getId(), 0L))
              .append('\n');
        }
        return csvResponse("users.csv", sb.toString());
    }

    /* ==================== SETTINGS ==================== */

    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        addSidebarAttributes(model);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        return "admin-settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String siteName,
                               @RequestParam String adminEmail,
                               @RequestParam double defaultBalance,
                               @RequestParam int defaultDuration,
                               RedirectAttributes ra) {
        // Pass submitted values back so the form reflects what was saved
        ra.addFlashAttribute("savedSiteName", siteName);
        ra.addFlashAttribute("savedAdminEmail", adminEmail);
        ra.addFlashAttribute("savedDefaultBalance", defaultBalance);
        ra.addFlashAttribute("savedDefaultDuration", defaultDuration);
        ra.addFlashAttribute("successMessage", "Settings saved. New accounts now start with $"
                + String.format("%,.2f", defaultBalance) + " and run for " + defaultDuration + " minutes.");
        return "redirect:/admin/settings";
    }

    /* ==================== BROADCAST ==================== */

    @GetMapping("/broadcast")
    public String broadcastPage(Model model, Principal principal) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        addSidebarAttributes(model);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));

        // Stats
        List<User> allUsers = userService.findAllUsers();
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("activeUsers", allUsers.stream().filter(User::isActive).count());

        // Count users who have bids (using repository query to avoid LazyInitException)
        long biddersCount = bidRepository.countBidsGroupedByBidder().size();
        model.addAttribute("topBiddersCount", biddersCount);

        // Broadcast stats from database
        long totalBroadcasts = broadcastRepository.count();
        long messagesDelivered = broadcastRepository.sumDeliveredMessages();
        long scheduledCount = broadcastRepository.countByScheduled(true);

        model.addAttribute("totalBroadcasts", totalBroadcasts);
        model.addAttribute("messagesDelivered", String.format("%,d", messagesDelivered));
        model.addAttribute("scheduledCount", scheduledCount);

        // Last broadcast time
        List<Broadcast> sentBroadcasts = broadcastRepository.findSentBroadcasts();
        if (!sentBroadcasts.isEmpty()) {
            model.addAttribute("lastBroadcastTime", sentBroadcasts.get(0).getTimeAgo());
        } else {
            model.addAttribute("lastBroadcastTime", "Never");
        }

        // Recent broadcasts (last 10)
        model.addAttribute("recentBroadcasts", sentBroadcasts.stream().limit(10).collect(Collectors.toList()));

        // Scheduled broadcasts
        model.addAttribute("scheduledBroadcasts", broadcastRepository.findScheduledBroadcasts());

        // All users for the "specific user" search
        model.addAttribute("allUsersList", allUsers);

        return "admin-broadcast";
    }

    @PostMapping("/broadcast/send")
    public String sendBroadcast(@RequestParam String type,
                                @RequestParam String subject,
                                @RequestParam String message,
                                @RequestParam(defaultValue = "all") String audience,
                                @RequestParam(required = false) List<Long> userIds,
                                @RequestParam(required = false) String scheduledFor,
                                Principal principal,
                                RedirectAttributes ra) {
        requireAdmin(principal);

        if (subject == null || subject.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Subject is required.");
            return "redirect:/admin/broadcast";
        }
        if (message == null || message.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Message is required.");
            return "redirect:/admin/broadcast";
        }

        // Parse scheduled time if provided
        LocalDateTime scheduledTime = null;
        boolean isScheduled = false;
        if (scheduledFor != null && !scheduledFor.trim().isEmpty()) {
            try {
                scheduledTime = LocalDateTime.parse(scheduledFor);
                if (scheduledTime.isAfter(LocalDateTime.now())) {
                    isScheduled = true;
                }
            } catch (Exception e) {
                // Invalid date format — ignore and send immediately
            }
        }

        // Build the notification message with type prefix
        String typePrefix = switch (type) {
            case "alert" -> "⚠️ ";
            case "update" -> "🔄 ";
            case "new_feature" -> "✨ ";
            default -> "📢 ";
        };
        String fullMessage = typePrefix + "**" + subject.trim() + "**\n\n" + message.trim();

        // Determine recipients based on audience
        List<User> allUsers = userService.findAllUsers();
        List<User> recipients;
        String audienceLabel;
        switch (audience) {
            case "active":
                recipients = allUsers.stream().filter(User::isActive).collect(java.util.stream.Collectors.toList());
                audienceLabel = recipients.size() + " active users";
                break;
            case "bidders":
                // Use repository query to avoid LazyInitializationException on user.getBids()
                Map<Long, Long> bidsPerUser = new HashMap<>();
                for (Object[] row : bidRepository.countBidsGroupedByBidder()) {
                    if (row[0] != null) bidsPerUser.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
                }
                recipients = allUsers.stream()
                        .filter(u -> bidsPerUser.containsKey(u.getId()))
                        .sorted((a, b) -> Long.compare(
                                bidsPerUser.getOrDefault(b.getId(), 0L),
                                bidsPerUser.getOrDefault(a.getId(), 0L)))
                        .limit(25)
                        .collect(java.util.stream.Collectors.toList());
                audienceLabel = recipients.size() + " top bidders";
                break;
            case "custom":
                if (userIds == null || userIds.isEmpty()) {
                    ra.addFlashAttribute("error", "Please select at least one user.");
                    return "redirect:/admin/broadcast";
                }
                recipients = allUsers.stream()
                        .filter(u -> userIds.contains(u.getId()))
                        .collect(java.util.stream.Collectors.toList());
                if (recipients.isEmpty()) {
                    ra.addFlashAttribute("error", "No matching users found.");
                    return "redirect:/admin/broadcast";
                }
                audienceLabel = recipients.size() + " selected user" + (recipients.size() > 1 ? "s" : "");
                break;
            default: // "all"
                recipients = allUsers;
                audienceLabel = "all " + recipients.size() + " users";
                break;
        }

        // Save broadcast record
        User adminUser = requireAdmin(principal);
        Broadcast broadcast = new Broadcast();
        broadcast.setSubject(subject.trim());
        broadcast.setMessage(message.trim());
        broadcast.setType(type);
        broadcast.setAudience(audience);
        broadcast.setSentBy(adminUser);

        if (isScheduled) {
            // Schedule for later — don't send now
            broadcast.setScheduled(true);
            broadcast.setScheduledFor(scheduledTime);
            broadcast.setRecipientCount(recipients.size());
            broadcast.setSentAt(null);
            // Store custom user IDs so the scheduler can send to the right people
            if ("custom".equals(audience) && userIds != null && !userIds.isEmpty()) {
                broadcast.setCustomUserIds(userIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
            }
            broadcastRepository.save(broadcast);

            ra.addFlashAttribute("success", "Broadcast scheduled for " + broadcast.getFormattedScheduledTime() + " (" + audienceLabel + ").");
            return "redirect:/admin/broadcast";
        }

        // Send immediately
        int sent = 0;
        for (User u : recipients) {
            notificationService.createNotification(u, fullMessage, "ANNOUNCEMENT", null);
            sent++;
        }

        broadcast.setRecipientCount(sent);
        broadcast.setSentAt(LocalDateTime.now());
        broadcast.setScheduled(false);
        broadcastRepository.save(broadcast);

        ra.addFlashAttribute("success", "Announcement delivered to " + audienceLabel + "!");
        return "redirect:/admin/broadcast";
    }

    @PostMapping("/broadcast/cancel/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String cancelScheduledBroadcast(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        requireAdmin(principal);
        Broadcast b = broadcastRepository.findById(id).orElse(null);
        if (b != null && b.isScheduled()) {
            broadcastRepository.delete(b);
            ra.addFlashAttribute("success", "Scheduled broadcast cancelled.");
        } else {
            ra.addFlashAttribute("error", "Broadcast not found or already sent.");
        }
        return "redirect:/admin/broadcast";
    }

    @PostMapping("/broadcast/extend/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String extendScheduledBroadcast(@PathVariable Long id,
                                           @RequestParam String newScheduledFor,
                                           Principal principal,
                                           RedirectAttributes ra) {
        requireAdmin(principal);
        Broadcast b = broadcastRepository.findById(id).orElse(null);
        if (b == null || !b.isScheduled()) {
            ra.addFlashAttribute("error", "Broadcast not found or already sent.");
            return "redirect:/admin/broadcast";
        }

        try {
            LocalDateTime newTime = LocalDateTime.parse(newScheduledFor);
            if (!newTime.isAfter(LocalDateTime.now())) {
                ra.addFlashAttribute("error", "New time must be in the future.");
                return "redirect:/admin/broadcast";
            }
            b.setScheduledFor(newTime);
            broadcastRepository.save(b);
            ra.addFlashAttribute("success", "Broadcast rescheduled to " + b.getFormattedScheduledTime() + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Invalid date format.");
        }
        return "redirect:/admin/broadcast";
    }

    @PostMapping("/notifications/broadcast")
    public String legacyBroadcast(@RequestParam String message, RedirectAttributes ra) {
        if (message == null || message.trim().isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Enter a message before broadcasting.");
            return "redirect:/admin/settings";
        }
        int sent = 0;
        for (User u : userService.findAllUsers()) {
            notificationService.createNotification(u, message.trim(), "ANNOUNCEMENT", null);
            sent++;
        }
        ra.addFlashAttribute("successMessage", "Announcement delivered to " + sent + " account" + (sent == 1 ? "" : "s") + ".");
        return "redirect:/admin/settings";
    }

    /* ==================== BIDS ==================== */

    @GetMapping("/bids")
    public String manageBids(Model model, Principal principal,
                             @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        addSidebarAttributes(model);
        
        List<Auction> allAuctions = auctionService.getAllAuctions();
        int totalBids = allAuctions.stream().mapToInt(Auction::getBidCount).sum();
        model.addAttribute("totalBids", totalBids);
        
        // Calculate bids today
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Bid> recentBidsAll = bidRepository.findBidsSince(startOfDay);
        model.addAttribute("bidsToday", recentBidsAll.size());
        
        // BUG B2 FIX: Calculate average bid amount from ALL individual bids, not just highest per auction
        List<Bid> allBidsList = bidRepository.findAll();
        double avgBidAmount = allBidsList.stream()
                .mapToDouble(Bid::getAmount)
                .average()
                .orElse(0.0);
        model.addAttribute("avgBidAmount", avgBidAmount);
        
        // BUG B1 FIX: Calculate real average time between bids
        String avgTimeStr = "N/A";
        if (allBidsList.size() >= 2) {
            // Sort by timestamp ascending for gap calculation
            List<Bid> sortedBids = allBidsList.stream()
                    .filter(b -> b.getTimestamp() != null)
                    .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                    .collect(java.util.stream.Collectors.toList());
            
            if (sortedBids.size() >= 2) {
                long totalGapSeconds = 0;
                int gapCount = 0;
                for (int i = 1; i < sortedBids.size(); i++) {
                    long gap = java.time.temporal.ChronoUnit.SECONDS.between(
                            sortedBids.get(i - 1).getTimestamp(), sortedBids.get(i).getTimestamp());
                    totalGapSeconds += gap;
                    gapCount++;
                }
                if (gapCount > 0) {
                    long avgSeconds = totalGapSeconds / gapCount;
                    if (avgSeconds < 60) {
                        avgTimeStr = avgSeconds + "s";
                    } else if (avgSeconds < 3600) {
                        avgTimeStr = (avgSeconds / 60) + "m " + (avgSeconds % 60) + "s";
                    } else {
                        avgTimeStr = (avgSeconds / 3600) + "h " + ((avgSeconds % 3600) / 60) + "m";
                    }
                }
            }
        }
        model.addAttribute("avgTimeBetweenBids", avgTimeStr);
        
        // BUG B4 FIX: Paginated recent bids (20 per page)
        int pageSize = 20;
        List<Bid> recentBidsRaw = bidRepository.findRecentBids(PageRequest.of(page, pageSize));
        long totalBidCount = bidRepository.count();
        int totalPages = (int) Math.ceil((double) totalBidCount / pageSize);
        
        List<Map<String, Object>> recentBids = new ArrayList<>();
        
        for (Bid bid : recentBidsRaw) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("bidder", bid.getBidder());
            b.put("auction", bid.getAuction());
            b.put("amount", bid.getAmount());
            b.put("timestamp", bid.getTimestamp());
            b.put("timeAgo", getTimeAgo(bid.getTimestamp()));
            
            // Check if this bid is the winning bid
            boolean isWinning = bid.getAuction().getHighestBidder() != null 
                    && bid.getAuction().getHighestBidder().getId().equals(bid.getBidder().getId())
                    && bid.getAmount() == bid.getAuction().getCurrentHighestBid();
            b.put("isWinning", isWinning);
            
            recentBids.add(b);
        }
        model.addAttribute("recentBids", recentBids);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalBidCount", totalBidCount);
        
        model.addAttribute("totalUsers", userService.findAllUsers().size());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        addCounts(model, allAuctions);
        addFormatters(model);
        
        return "admin-bids";
    }

    @GetMapping("/export/bids.csv")
    public ResponseEntity<byte[]> exportBids() {
        StringBuilder sb = new StringBuilder("id,bidder,auction,amount,timestamp\n");
        List<Bid> allBids = bidRepository.findAll();
        for (Bid b : allBids) {
            sb.append(b.getId()).append(',')
              .append(csv(b.getBidder().getUsername())).append(',')
              .append(csv(b.getAuction().getTitle())).append(',')
              .append(fmtMoney(b.getAmount())).append(',')
              .append(b.getTimestamp() != null ? b.getTimestamp().format(DATE_TIME) : "")
              .append('\n');
        }
        return csvResponse("bids.csv", sb.toString());
    }

    /* ==================== CHAT MONITORING ==================== */

    /**
     * GET /admin/chat/{conversationId}
     * Admin read-only view of chat conversation (for dispute resolution).
     */
    @GetMapping("/chat/{conversationId}")
    public String viewChatConversation(@PathVariable Long conversationId,
                                         Model model, Principal principal) {
        User admin = requireAdmin(principal);
        model.addAttribute("admin", admin);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));

        try {
            List<ChatMessage> messages = chatService.getMessages(conversationId, admin);
            if (!messages.isEmpty()) {
                Conversation conversation = messages.get(0).getConversation();
                model.addAttribute("conversation", conversation);
            }
            model.addAttribute("messages", messages);
            model.addAttribute("dateFormatter", DATE_TIME);
            addCounts(model, auctionService.getAllAuctions());
        } catch (SecurityException e) {
            model.addAttribute("error", "Access denied.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Conversation not found.");
        }

        return "admin-chat-view";
    }

    /* ==================== HELPERS ==================== */

    private User requireAdmin(Principal principal) {
        if (principal == null) throw new IllegalStateException("No authenticated principal");
        return userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Admin account not found: " + principal.getName()));
    }

    private void addFormatters(Model model) {
        model.addAttribute("dateFormatter", DATE);
        model.addAttribute("dateTimeFormatter", DATE_TIME);
    }

    private void addCounts(Model model, List<Auction> auctions) {
        model.addAttribute("countAll", auctions.size());
        model.addAttribute("countActive", auctions.stream().filter(a -> a.getStatus() == AuctionStatus.ACTIVE).count());
        model.addAttribute("countClosed", auctions.stream().filter(a -> a.getStatus() == AuctionStatus.CLOSED).count());
        model.addAttribute("countCancelled", auctions.stream().filter(a -> a.getStatus() == AuctionStatus.CANCELLED).count());
    }

    /**
     * Adds model attributes required by the shared admin sidebar fragment.
     * Call this from every admin page controller method.
     */
    private void addSidebarAttributes(Model model) {
        List<Auction> allAuctions = auctionService.getAllAuctions();
        model.addAttribute("totalAuctions", allAuctions.size());
        model.addAttribute("pendingCount", auctionService.getPendingAuctions().size());
        model.addAttribute("pendingPaymentCount", paymentReleaseService.countPendingReview());
        model.addAttribute("totalUsers", userService.findAllUsers().size());
    }

    /** Category counts with display names and percentage of the total. */
    private List<Map<String, Object>> breakdown(List<Auction> auctions) {
        List<Map<String, Object>> out = new ArrayList<>();
        int total = auctions.size();
        for (AuctionCategory c : AuctionCategory.values()) {
            long count = auctions.stream().filter(a -> a.getCategory() == c).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", c.name());
            m.put("name", c.getDisplayName());
            m.put("count", count);
            m.put("pct", total == 0 ? 0 : Math.round(count * 100.0 / total));
            out.add(m);
        }
        out.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        return out;
    }

    private List<Map<String, Object>> statusBreakdown(List<Auction> auctions) {
        List<Map<String, Object>> out = new ArrayList<>();
        int total = auctions.size();
        for (AuctionStatus s : AuctionStatus.values()) {
            long count = auctions.stream().filter(a -> a.getStatus() == s).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", s.name());
            m.put("name", s.name().charAt(0) + s.name().substring(1).toLowerCase());
            m.put("count", count);
            m.put("pct", total == 0 ? 0 : Math.round(count * 100.0 / total));
            out.add(m);
        }
        return out;
    }

    /** Closed-auction value per month for the last six months. */
    private Map<String, Double> monthlyRevenue(List<Auction> auctions) {
        Map<String, Double> out = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            String label = start.format(DateTimeFormatter.ofPattern("MMM"));
            double v = auctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.CLOSED && a.getEndTime() != null
                            && !a.getEndTime().isBefore(start) && !a.getEndTime().isAfter(end))
                    .mapToDouble(Auction::getCurrentHighestBid).sum();
            out.put(label, v);
        }
        return out;
    }

    private static String fmtMoney(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String csv(String v) {
        if (v == null) return "";
        String s = v;
        // Formula injection guard: prefix with single quote if starts with = + - @
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        s = s.replace("\"", "\"\"");
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s + "\"" : s;
    }

    private static ResponseEntity<byte[]> csvResponse(String filename, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
    }

    /* ==================== EDIT AUCTION ==================== */

    @GetMapping("/auctions/edit/{id}")
    public String editAuctionForm(@PathVariable Long id, Model model, Principal principal) {
        User admin = requireAdmin(principal);
        Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        model.addAttribute("admin", admin);
        model.addAttribute("auction", auction);
        model.addAttribute("categories", AuctionCategory.values());
        model.addAttribute("hasBids", auction.getBidCount() > 0);
        addSidebarAttributes(model);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(admin.getId()));
        return "admin/edit-auction";
    }

    @PostMapping("/auctions/edit/{id}")
    public String editAuction(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam String description,
                               @RequestParam(required = false, defaultValue = "0") double startingPrice,
                               @RequestParam(required = false) String imageUrl,
                               @RequestParam(required = false) MultipartFile auctionImage,
                               @RequestParam(required = false) String carMake,
                               @RequestParam(required = false) String carModel,
                               @RequestParam(required = false, defaultValue = "0") int carYear,
                               @RequestParam(required = false, defaultValue = "0") int carMileage,
                               @RequestParam(required = false) String carCondition,
                               @RequestParam(required = false) String carColor,
                               @RequestParam(required = false) String watchBrand,
                               @RequestParam(required = false) String watchModelName,
                               @RequestParam(required = false) String watchMovement,
                               @RequestParam(required = false) String watchCaseMaterial,
                               @RequestParam(required = false) String watchReference,
                               @RequestParam(required = false) String jewelryMetal,
                               @RequestParam(required = false) String jewelryGemstone,
                               @RequestParam(required = false, defaultValue = "0") double jewelryCarat,
                               @RequestParam(required = false) String jewelryDesigner,
                               @RequestParam(required = false, defaultValue = "false") boolean jewelryCertified,
                               @RequestParam(required = false) String artArtist,
                               @RequestParam(required = false) String artMedium,
                               @RequestParam(required = false, defaultValue = "0") int artYearCreated,
                               @RequestParam(required = false) String artDimensions,
                               @RequestParam(required = false, defaultValue = "false") boolean artAuthenticated,
                               @RequestParam(required = false) String colSubcategory,
                               @RequestParam(required = false) String colEra,
                               @RequestParam(required = false) String colCondition,
                               @RequestParam(required = false) String colRarity,
                               @RequestParam(required = false) String colProvenance,
                               RedirectAttributes ra) {
        try {
            Auction auction = auctionService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Auction not found"));

            auction.setTitle(title.trim());
            auction.setDescription(description.trim());

            if (auction.getBidCount() == 0) {
                auction.setStartingPrice(startingPrice);
            }

            if (auctionImage != null && !auctionImage.isEmpty()) {
                String filename = fileStorageService.storeAuctionImage(auctionImage);
                auction.setImageUrl("/uploads/auctions/" + filename);
            } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                auction.setImageUrl(imageUrl.trim());
            }

            AuctionCategory cat = auction.getCategory();
            switch (cat) {
                case CARS:
                    if (auction instanceof com.auctionhouse.model.CarAuction) {
                        com.auctionhouse.model.CarAuction ca = (com.auctionhouse.model.CarAuction) auction;
                        if (carMake != null && !carMake.trim().isEmpty()) ca.setMake(carMake.trim());
                        if (carModel != null && !carModel.trim().isEmpty()) ca.setModel(carModel.trim());
                        if (carYear > 0) ca.setYear(carYear);
                        ca.setMileage(carMileage);
                        if (carCondition != null && !carCondition.trim().isEmpty()) ca.setCondition(carCondition.trim());
                        if (carColor != null && !carColor.trim().isEmpty()) ca.setColor(carColor.trim());
                    }
                    break;
                case WATCHES:
                    if (auction instanceof com.auctionhouse.model.WatchAuction) {
                        com.auctionhouse.model.WatchAuction wa = (com.auctionhouse.model.WatchAuction) auction;
                        if (watchBrand != null && !watchBrand.trim().isEmpty()) wa.setBrand(watchBrand.trim());
                        if (watchModelName != null && !watchModelName.trim().isEmpty()) wa.setModelName(watchModelName.trim());
                        if (watchMovement != null && !watchMovement.trim().isEmpty()) wa.setMovement(watchMovement.trim());
                        if (watchCaseMaterial != null && !watchCaseMaterial.trim().isEmpty()) wa.setCaseMaterial(watchCaseMaterial.trim());
                        if (watchReference != null && !watchReference.trim().isEmpty()) wa.setReferenceNumber(watchReference.trim());
                    }
                    break;
                case JEWELRY:
                    if (auction instanceof com.auctionhouse.model.JewelryAuction) {
                        com.auctionhouse.model.JewelryAuction ja = (com.auctionhouse.model.JewelryAuction) auction;
                        if (jewelryMetal != null && !jewelryMetal.trim().isEmpty()) ja.setMetalType(jewelryMetal.trim());
                        if (jewelryGemstone != null && !jewelryGemstone.trim().isEmpty()) ja.setGemstone(jewelryGemstone.trim());
                        if (jewelryCarat > 0) ja.setCarat(jewelryCarat);
                        if (jewelryDesigner != null && !jewelryDesigner.trim().isEmpty()) ja.setDesigner(jewelryDesigner.trim());
                        ja.setCertified(jewelryCertified);
                    }
                    break;
                case ART:
                    if (auction instanceof com.auctionhouse.model.ArtAuction) {
                        com.auctionhouse.model.ArtAuction aa = (com.auctionhouse.model.ArtAuction) auction;
                        if (artArtist != null && !artArtist.trim().isEmpty()) aa.setArtist(artArtist.trim());
                        if (artMedium != null && !artMedium.trim().isEmpty()) aa.setMedium(artMedium.trim());
                        if (artYearCreated > 0) aa.setYearCreated(artYearCreated);
                        if (artDimensions != null && !artDimensions.trim().isEmpty()) aa.setDimensions(artDimensions.trim());
                        aa.setAuthenticated(artAuthenticated);
                    }
                    break;
                case COLLECTIBLES:
                    if (auction instanceof com.auctionhouse.model.CollectibleAuction) {
                        com.auctionhouse.model.CollectibleAuction co = (com.auctionhouse.model.CollectibleAuction) auction;
                        if (colSubcategory != null && !colSubcategory.trim().isEmpty()) co.setSubcategory(colSubcategory.trim());
                        if (colEra != null && !colEra.trim().isEmpty()) co.setEra(colEra.trim());
                        if (colCondition != null && !colCondition.trim().isEmpty()) co.setCondition(colCondition.trim());
                        if (colRarity != null && !colRarity.trim().isEmpty()) co.setRarity(colRarity.trim());
                        if (colProvenance != null && !colProvenance.trim().isEmpty()) co.setProvenance(colProvenance.trim());
                    }
                    break;
            }

            auctionService.save(auction);
            ra.addFlashAttribute("successMessage", "Auction updated successfully!");
            return "redirect:/admin/auctions";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update auction: " + e.getMessage());
            return "redirect:/admin/auctions/edit/" + id;
        }
    }

}
