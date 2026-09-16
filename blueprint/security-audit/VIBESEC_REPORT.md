# 🔒 VibeSec Security Audit Report

**Date:** 2026-09-16  
**Auditor:** VibeSec Skill (Automated Security Review)  
**Target:** Auction House Simulator — Full Codebase  
**Framework:** Spring Boot 2.7.x + Spring Security 5 + Thymeleaf + H2  
**Files Audited:** 68 Java files, 30+ templates, 3 JS files, 1 properties file

---

## Executive Summary

| Severity | Count | Status |
|----------|-------|--------|
| 🔴 CRITICAL | 3 | Need immediate fix |
| 🟠 HIGH | 5 | Should fix before production |
| 🟡 MODERATE | 6 | Recommended improvements |
| 🟢 LOW | 3 | Nice to have |
| **Total** | **17** | |

**Overall Security Score: 6.5/10** — Good foundation but several critical gaps for production readiness.

---

## 🔴 CRITICAL Vulnerabilities

### V-001: H2 Console Exposed with Remote Access Enabled

**File:** `src/main/resources/application.properties`  
**Severity:** CRITICAL  
**CVSS:** 9.8

**Finding:**
```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=true
```

The H2 database console is enabled with remote access. Combined with `permitAll()` for `/h2-console/**` in SecurityConfig, **anyone on the internet can access the database console** and execute arbitrary SQL.

**Impact:** Full database compromise — read/write/delete all data, extract user credentials.

**Fix:**
```properties
# Production: Disable H2 console entirely
spring.h2.console.enabled=false

# If needed for dev only, remove web-allow-others
spring.h2.console.settings.web-allow-others=false
```

Also restrict in SecurityConfig:
```java
// Remove /h2-console/** from permitAll
// Add profile-specific config:
@Profile("dev")
.antMatchers("/h2-console/**").hasRole("SUPER_ADMIN")
```

---

### V-002: CSRF Disabled for All API Endpoints

**File:** `SecurityConfig.java:73`  
**Severity:** CRITICAL  
**CVSS:** 8.1

**Finding:**
```java
.csrf().ignoringAntMatchers("/h2-console/**", "/api/**", "/login", "/register", "/logout", "/ws/**");
```

The pattern `/api/**` disables CSRF protection for **all** API endpoints. Additionally, `/login` and `/register` should NOT be excluded from CSRF — this enables **login CSRF** attacks.

**Impact:**
- An attacker can forge requests to any `/api/**` endpoint on behalf of an authenticated user
- Login CSRF: Attacker logs victim into attacker-controlled account to track victim's activity
- Registration CSRF: Attacker registers an account with victim's email

**Fix:**
```java
.csrf().ignoringAntMatchers("/h2-console/**", "/ws/**")
// Remove /api/**, /login, /register, /logout from ignore list
// If you have a REST API that uses token auth, add CSRF token validation via headers instead
```

---

### V-003: Sequential IDs Enable IDOR Attacks

**File:** All model classes (9 entities)  
**Severity:** CRITICAL  
**CVSS:** 7.5

**Finding:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

All entities use auto-incrementing sequential IDs (1, 2, 3...). This makes **Insecure Direct Object Reference (IDOR)** attacks trivial — an attacker can simply increment/decrement IDs to access other users' data.

**Affected Entities:** User, Auction, Bid, Conversation, ChatMessage, PaymentRelease, PlatformCommission, Notification, AdminAuditLog

**Impact:**
- User enumeration (iterate through `/profile/{id}`, `/admin/chat/{id}`)
- Auction data access (iterate through `/auctions/detail/{id}`)
- Chat conversation access (iterate through `/chat/{id}/messages`)
- Payment release access (iterate through `/admin/payments/{id}`)

**Current Mitigations (Partial):**
- ✅ ChatService validates participant access
- ✅ AdminController requires ADMIN role
- ✅ ProfileController uses authenticated user (no ID parameter)
- ❌ Auction detail page allows viewing any auction by ID
- ❌ No ownership validation on auction detail for active auctions

**Fix (Long-term):**
```java
// Use UUIDs for all entity IDs
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

**Fix (Short-term):** Add ownership validation middleware for all endpoints accepting IDs.

---

## 🟠 HIGH Severity

### V-004: Missing Security Headers

**File:** `SecurityConfig.java`  
**Severity:** HIGH  
**CVSS:** 6.5

**Finding:** No security headers are configured. The application is missing:

| Header | Status | Purpose |
|--------|--------|---------|
| `Content-Security-Policy` | ❌ Missing | Prevents XSS, data injection |
| `X-Content-Type-Options` | ❌ Missing | Prevents MIME sniffing |
| `Strict-Transport-Security` | ❌ Missing | Enforces HTTPS |
| `Referrer-Policy` | ❌ Missing | Controls referrer leakage |
| `X-Frame-Options` | ⚠️ Partial | Set to `sameOrigin` (good) but should be `DENY` |

**Fix:**
```java
http.headers()
    .contentSecurityPolicy("default-src 'self'; script-src 'self' https://cdn.tailwindcss.com https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; frame-ancestors 'none';")
    .and()
    .contentTypeOptions() // X-Content-Type-Options: nosniff
    .and()
    .httpStrictTransportSecurity() // HSTS
    .and()
    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .and()
    .frameOptions().deny(); // Change from sameOrigin to DENY
```

---

### V-005: File Upload — No Magic Bytes Validation

**File:** `FileStorageService.java:82-94`  
**Severity:** HIGH  
**CVSS:** 7.0

**Finding:**
```java
private void validateImage(MultipartFile file) throws IOException {
    String contentType = file.getContentType();
    if (contentType == null || (!contentType.equals("image/jpeg") &&
            !contentType.equals("image/png") && !contentType.equals("image/gif") &&
            !contentType.equals("image/webp"))) {
        throw new IOException("Only JPEG, PNG, GIF, and WebP images are allowed");
    }
}
```

The validation only checks the `Content-Type` header, which is **client-controlled** and trivially spoofable. An attacker can upload a malicious file (e.g., a JSP webshell) by setting `Content-Type: image/jpeg` on a `.jsp` file.

**Missing:**
- ❌ Magic bytes validation (file signature)
- ❌ File extension validation
- ❌ File content parsing (image processing validation)

**Impact:** If combined with a path traversal or misconfigured server, could lead to Remote Code Execution (RCE).

**Current Mitigations:**
- ✅ Files are renamed with UUID (prevents filename injection)
- ✅ Extension is derived from Content-Type (not original filename)
- ✅ Files stored outside webroot for profiles
- ⚠️ Auction images and proof files served via resource handler

**Fix:**
```java
private void validateImage(MultipartFile file) throws IOException {
    if (file.isEmpty()) throw new IOException("File is empty");
    if (file.getSize() > 5 * 1024 * 1024) throw new IOException("File size must be less than 5MB");
    
    // Read magic bytes
    byte[] header = new byte[8];
    try (InputStream is = file.getInputStream()) {
        is.read(header);
    }
    
    // Validate magic bytes match claimed content type
    String contentType = file.getContentType();
    boolean valid = false;
    
    if ("image/jpeg".equals(contentType) && header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF) {
        valid = true;
    } else if ("image/png".equals(contentType) && header[0] == (byte)0x89 && header[1] == (byte)0x50) {
        valid = true;
    } else if ("image/gif".equals(contentType) && header[0] == (byte)0x47 && header[1] == (byte)0x49) {
        valid = true;
    } else if ("image/webp".equals(contentType) && header[0] == (byte)0x52 && header[1] == (byte)0x49) {
        valid = true;
    }
    
    if (!valid) throw new IOException("File content does not match claimed type");
}
```

---

### V-006: Open Redirect in Search

**File:** `AuctionController.java:109`  
**Severity:** HIGH  
**CVSS:** 6.1

**Finding:**
```java
@GetMapping("/search")
public String search(@RequestParam String q, Model model) {
    return "redirect:/auctions?search=" + q;
}
```

The `q` parameter is appended directly to the redirect URL without sanitization. An attacker can craft a URL like:
- `/auctions/search?q=//evil.com` → redirects to `//evil.com`
- `/auctions/search?q=%0d%0aSet-Cookie:...` → header injection

**Fix:**
```java
@GetMapping("/search")
public String search(@RequestParam String q, Model model) {
    // Sanitize: only allow safe characters, strip protocol-relative URLs
    String sanitized = q.replaceAll("[^a-zA-Z0-9 _\\-]", "");
    return "redirect:/auctions?search=" + UriUtils.encode(sanitized, StandardCharsets.UTF_8);
}
```

---

### V-007: Login CSRF — No Protection

**File:** `SecurityConfig.java:73`  
**Severity:** HIGH  
**CVSS:** 5.3

**Finding:**
```java
.csrf().ignoringAntMatchers(..., "/login", "/register", ...)
```

Login and registration endpoints are excluded from CSRF protection. This enables:
- **Login CSRF:** Attacker logs victim into attacker's account, then victim's actions are tracked
- **Registration CSRF:** Attacker registers an account with victim's email address

**Fix:** Remove `/login` and `/register` from the CSRF ignore list. Spring Security's form login handles CSRF tokens automatically.

---

### V-008: Error Messages Leak Internal Details

**File:** Multiple controllers  
**Severity:** HIGH  
**CVSS:** 5.3

**Finding:**
```java
// AuctionController.java
redirectAttributes.addFlashAttribute("error", "Failed to create auction: " + e.getMessage());

// AdminController.java
ra.addFlashAttribute("errorMessage", "Failed to create user: " + e.getMessage());

// ProfileController.java
redirectAttributes.addFlashAttribute("error", "Failed to upload: " + e.getMessage());
```

Exception messages are exposed directly to users. This can leak:
- Database table/column names
- File system paths
- Stack trace fragments
- Internal service URLs

**Fix:** Use generic error messages and log the details server-side:
```java
} catch (Exception e) {
    log.error("Failed to create auction", e);
    redirectAttributes.addFlashAttribute("error", "Failed to create auction. Please try again.");
    return "redirect:/auctions/create";
}
```

---

## 🟡 MODERATE Severity

### V-009: Weak Password Requirements

**File:** `UserRegistrationDTO.java`  
**Severity:** MODERATE  
**CVSS:** 4.3

**Finding:**
```java
@Size(min = 6, message = "Password must be at least 6 characters")
private String password;
```

Minimum password length is 6 characters. VibeSec recommends **8 minimum, 12+ recommended**.

**Fix:**
```java
@Size(min = 8, max = 128, message = "Password must be 8-128 characters")
```

---

### V-010: No Account Lockout / Brute Force Protection

**File:** `SecurityConfig.java`  
**Severity:** MODERATE  
**CVSS:** 4.0

**Finding:** No rate limiting or account lockout on the login endpoint. An attacker can perform unlimited brute force attempts.

**Fix:** Implement rate limiting:
```java
// Add to SecurityConfig or use a filter
@Bean
public Filter rateLimitFilter() {
    // Limit to 5 failed attempts per IP per 15 minutes
}
```

---

### V-011: Session Cookie SameSite=None

**File:** `application.properties`  
**Severity:** MODERATE  
**CVSS:** 4.0

**Finding:**
```properties
server.servlet.session.cookie.same-site=none
```

`SameSite=None` is the weakest setting — cookies are sent with all cross-site requests. This is likely needed for the Railway preview proxy, but should be `Lax` or `Strict` in production.

**Fix:** Use profile-specific configuration:
```properties
# application-dev.properties
server.servlet.session.cookie.same-site=none

# application-prod.properties
server.servlet.session.cookie.same-site=strict
```

---

### V-012: Unlimited Demo Money (No Rate Limiting)

**File:** `ProfileController.java:163`  
**Severity:** MODERATE  
**CVSS:** 3.5

**Finding:**
```java
@PostMapping("/profile/add-money")
public String addDemoMoney(@AuthenticationPrincipal UserDetails userDetails, ...) {
    user.setWalletBalance(user.getWalletBalance() + 50000.0);
    userService.updateProfile(user);
    ...
}
```

No rate limiting on demo money claims. While this is a demo application and the user explicitly requested unlimited demo money, in production this would be a business logic vulnerability.

**Status:** Known and accepted by user for demo purposes.

---

### V-013: No Mass Assignment Protection on Profile Update

**File:** `UserService.java:113`  
**Severity:** MODERATE  
**CVSS:** 5.0

**Finding:**
```java
@Transactional
public User updateProfile(User user) {
    return userRepository.save(user);
}
```

The `updateProfile` method saves the entire User object. If any controller passes a User object that was populated from request parameters without field filtering, an attacker could modify protected fields like `role`, `walletBalance`, or `active`.

**Current Mitigations:**
- ✅ ProfileController only updates profile pic path
- ✅ AdminController manually sets individual fields
- ⚠️ The method itself is unsafe if misused

**Fix:**
```java
@Transactional
public User updateProfile(User user, String[] allowedFields) {
    User existing = userRepository.findById(user.getId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    // Only update allowed fields
    if (allowedFields.contains("email")) existing.setEmail(user.getEmail());
    if (allowedFields.contains("profilePicPath")) existing.setProfilePicPath(user.getProfilePicPath());
    return userRepository.save(existing);
}
```

---

### V-014: @Scheduled Cleanup Has No Transaction Isolation

**File:** `ChatService.java:143`  
**Severity:** MODERATE  
**CVSS:** 3.0

**Finding:**
```java
@Scheduled(cron = "0 0 3 * * ?")
@Transactional
public void scheduledCleanup() {
    int deleted = cleanupExpiredMessages();
}
```

The cleanup runs with default transaction isolation. In a multi-instance deployment, multiple instances could run the cleanup simultaneously, causing deadlocks or duplicate work.

**Fix:** Use `@SchedulerLock` (ShedLock) or a distributed lock:
```java
@SchedulerLock(name = "chatCleanup", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
@Scheduled(cron = "0 0 3 * * ?")
public void scheduledCleanup() { ... }
```

---

## 🟢 LOW Severity

### V-015: innerHTML Usage in Chat Client

**File:** `chat-client.js:139`  
**Severity:** LOW  
**CVSS:** 2.0

**Finding:**
```javascript
messageDiv.innerHTML = '<div ...>' + escapeHtml(msg.content) + '</div>';
```

The `innerHTML` property is used to render chat messages. While the `escapeHtml()` function properly sanitizes content, `innerHTML` is inherently risky — a future code change could forget to escape.

**Current Mitigation:** ✅ `escapeHtml()` is applied to all user-controlled content.

**Fix:** Use `textContent` and DOM methods:
```javascript
const p = document.createElement('p');
p.textContent = msg.content;
messageDiv.appendChild(p);
```

---

### V-016: H2 Database Password is Empty

**File:** `application.properties`  
**Severity:** LOW  
**CVSS:** 2.0

**Finding:**
```properties
spring.datasource.username=sa
spring.datasource.password=
```

The H2 database has no password. While H2 is file-based and not exposed over the network, this is still a weak configuration.

**Fix:** Set a strong password for H2, or use environment variables:
```properties
spring.datasource.password=${DB_PASSWORD:changeme}
```

---

### V-017: No @EnableScheduling Verification

**File:** `AuctionHouseApplication.java`  
**Severity:** LOW  
**CVSS:** 1.0

**Finding:** The `@Scheduled` annotations on `AuctionService.closeExpiredAuctions()` and `ChatService.scheduledCleanup()` require `@EnableScheduling` to be present. If it's missing, scheduled tasks silently won't run.

**Fix:** Verify `@EnableScheduling` is present on the main application class.

---

## ✅ Security Strengths (What's Done Right)

| Area | Status | Notes |
|------|--------|-------|
| **Password Storage** | ✅ BCrypt | Proper hashing algorithm |
| **SQL Injection** | ✅ JPA/Hibernate | All queries use parameterized JPQL |
| **XSS (Templates)** | ✅ Thymeleaf | `th:text` auto-escapes, no `th:utext` found |
| **XSS (Chat JS)** | ✅ escapeHtml() | All user content escaped before innerHTML |
| **File Upload Naming** | ✅ UUID rename | Original filenames discarded |
| **Access Control (Admin)** | ✅ @PreAuthorize | Role-based access on admin endpoints |
| **Access Control (Chat)** | ✅ Service-layer | Participant validation in ChatService |
| **Access Control (Super Admin)** | ✅ @PreAuthorize | SUPER_ADMIN role check |
| **Bid Validation** | ✅ Server-side | Amount, balance, auction status validated |
| **Pessimistic Locking** | ✅ findByIdForUpdate | Prevents race conditions on bids |
| **BigDecimal Money Math** | ✅ HALF_UP | Proper rounding for financial calculations |
| **Self-Bid Prevention** | ✅ Checked | Seller cannot bid on own auction |
| **Consecutive Bid Prevention** | ✅ Checked | User cannot bid twice in a row |
| **Audit Logging** | ✅ AdminAuditLog | Super admin actions logged |
| **Deactivated User Blocking** | ✅ Login blocked | Deactivated users cannot authenticate |
| **CSRF on WebSocket** | ✅ Disabled | Appropriate for WebSocket protocol |
| **Error Handling** | ✅ GlobalExceptionHandler | Generic error page for unhandled exceptions |

---

## Remediation Priority

### Immediate (Before Production)
1. **V-001:** Disable H2 console in production
2. **V-002:** Remove `/api/**`, `/login`, `/register` from CSRF ignore
3. **V-004:** Add security headers (CSP, HSTS, X-Content-Type-Options)
4. **V-005:** Add magic bytes validation to file uploads
5. **V-006:** Fix open redirect in search
6. **V-008:** Sanitize error messages

### Short-term (Next Sprint)
7. **V-003:** Plan UUID migration for entity IDs
8. **V-009:** Increase minimum password length to 8
9. **V-010:** Add rate limiting on login
10. **V-011:** Profile-specific session cookie config
11. **V-013:** Add field filtering to updateProfile

### Long-term (Tech Debt)
12. **V-007:** Implement login CSRF protection
13. **V-014:** Add distributed lock for scheduled tasks
14. **V-015:** Replace innerHTML with DOM methods
15. **V-016:** Set H2 password
16. **V-017:** Verify @EnableScheduling

---

## Conclusion

The application has a **solid security foundation** with proper password hashing, parameterized queries, XSS protection via Thymeleaf, and role-based access control. However, there are **3 critical vulnerabilities** that must be addressed before production deployment:

1. **H2 console exposure** — immediate data breach risk
2. **Overly broad CSRF disable** — enables cross-site request forgery
3. **Sequential IDs** — enables IDOR attacks

The most impactful quick wins are:
- Disable H2 console (1 line change)
- Add security headers (5 lines of code)
- Fix the open redirect (2 lines of code)
- Add magic bytes validation (15 lines of code)

**Estimated effort to fix all critical/high issues: 2-3 hours**
