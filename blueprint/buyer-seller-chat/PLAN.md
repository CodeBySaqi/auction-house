# 🏗️ Implementation Plan: Buyer-Seller Chat

**Feature:** Real-time text chat between buyer and seller after auction is won
**Generated:** Blueprint Q&A → Plan

---

## Overview

When an auction closes with a winner, a `Conversation` is auto-created alongside the existing `PaymentRelease`. Both parties see an embedded chat panel on the seller/buyer payment submission pages. Messages are delivered in real-time via WebSocket (STOMP over SockJS). Admins can view chat history (read-only) for dispute resolution. Messages are auto-deleted 90 days after payment release.

---

## Phase 1: Database Models

### 1.1 Create `Conversation` entity

**File:** `src/main/java/com/auctionhouse/model/Conversation.java`

```java
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_release_id", nullable = false, unique = true)
    private PaymentRelease paymentRelease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    // Constructor sets createdAt = now
    // Getters and setters
}
```

**Key design decisions:**
- `@OneToOne` with `PaymentRelease` — one conversation per won auction
- `buyer` and `seller` denormalized for fast queries (avoid joining through PaymentRelease)
- `messages` collection lazy-loaded, ordered by creation time

### 1.2 Create `ChatMessage` entity

**File:** `src/main/java/com/auctionhouse/model/ChatMessage.java`

```java
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructor sets createdAt = now
    // Getters and setters
}
```

**Key design decisions:**
- `content` limited to 2000 chars (text-only, no attachments)
- `sender` is a `User` reference (not just ID) for easy display
- No `isRead` field — keeping it simple as agreed (no read receipts)

---

## Phase 2: Repositories

### 2.1 `ConversationRepository`

**File:** `src/main/java/com/auctionhouse/repository/ConversationRepository.java`

```java
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByPaymentRelease(PaymentRelease paymentRelease);
    Optional<Conversation> findByAuction(Auction auction);
    boolean existsByAuction(Auction auction);

    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);
}
```

### 2.2 `ChatMessageRepository`

**File:** `src/main/java/com/auctionhouse/repository/ChatMessageRepository.java`

```java
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);

    // For cleanup job
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation IN " +
           "(SELECT c FROM Conversation c JOIN c.paymentRelease pr " +
           "WHERE pr.paymentReleased = true AND pr.releasedAt < :cutoff)")
    List<ChatMessage> findExpiredMessages(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversation IN " +
           "(SELECT c FROM Conversation c JOIN c.paymentRelease pr " +
           "WHERE pr.paymentReleased = true AND pr.releasedAt < :cutoff)")
    int deleteExpiredMessages(@Param("cutoff") LocalDateTime cutoff);
}
```

---

## Phase 3: WebSocket Configuration

### 3.1 Add dependency to `pom.xml`

```xml
<!-- WebSocket (STOMP over SockJS) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 3.2 Create `WebSocketConfig`

**File:** `src/main/java/com/auctionhouse/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Clients subscribe to /topic/chat/{conversationId} to receive messages
        config.enableSimpleBroker("/topic");
        // Clients send to /app/chat (routed to @MessageMapping methods)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Allow Railway preview domains
                .withSockJS();                  // Fallback for browsers without WebSocket
    }
}
```

**Key decisions:**
- `/ws` endpoint with SockJS fallback for older browsers
- `/topic/chat/{conversationId}` — subscription channel per conversation
- `/app/chat.send` — client sends messages here
- `setAllowedOriginPatterns("*")` — needed for Railway's dynamic preview domains

### 3.3 WebSocket Security

**File:** `src/main/java/com/auctionhouse/config/WebSocketSecurityConfig.java`

```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected boolean sameOriginDisabled() {
        // Disable CSRF for WebSocket — STOMP uses the session cookie for auth
        // and SockJS requires cross-origin access for fallback transports
        return true;
    }

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpTypeMessageMappings(SimpMessageType.CONNECT).authenticated()
            .simpDestMatchers("/app/**").authenticated()
            .simpSubscribeDestMatchers("/topic/**").authenticated()
            .anyMessage().denyAll();
    }
}
```

**Note:** If `AbstractSecurityWebSocketMessageBrokerConfigurer` is deprecated in Spring Security 5.7+, use the `@EnableWebSocketSecurity` approach or configure via `SecurityFilterChain`. For Spring Boot 2.7.x, the above works.

### 3.4 Update `SecurityConfig.java`

Add to existing `filterChain()`:

```java
.antMatchers("/ws/**").permitAll()  // WebSocket handshake endpoint
```

---

## Phase 4: Service Layer

### 4.1 Create `ChatService`

**File:** `src/main/java/com/auctionhouse/service/ChatService.java`

```java
@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PaymentReleaseRepository paymentReleaseRepository;
    private final UserRepository userRepository;

    // Constructor injection

    /**
     * Create a conversation for a won auction. Called from PaymentReleaseService.
     * Idempotent — returns existing conversation if one already exists.
     */
    @Transactional
    public Conversation createConversation(PaymentRelease paymentRelease) {
        return conversationRepository.findByPaymentRelease(paymentRelease)
            .orElseGet(() -> {
                Conversation c = new Conversation();
                c.setPaymentRelease(paymentRelease);
                c.setAuction(paymentRelease.getAuction());
                c.setBuyer(paymentRelease.getBuyer());
                c.setSeller(paymentRelease.getSeller());
                return conversationRepository.save(c);
            });
    }

    /**
     * Get conversation by auction ID.
     */
    public Optional<Conversation> findByAuctionId(Long auctionId) {
        Auction auction = new Auction(); // placeholder
        return conversationRepository.findByAuction(
            // Actually query by auction ID through PaymentRelease
        );
        // Better: add findByAuctionId to ConversationRepository
    }

    /**
     * Send a message. Validates sender is participant.
     * Returns the saved message for broadcasting.
     */
    @Transactional
    public ChatMessage sendMessage(Long conversationId, Long senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        // SECURITY: Only buyer or seller can send messages
        if (!sender.getId().equals(conversation.getBuyer().getId())
            && !sender.getId().equals(conversation.getSeller().getId())) {
            throw new SecurityException("You are not a participant in this conversation.");
        }

        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Message too long (max 2000 characters).");
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        return chatMessageRepository.save(message);
    }

    /**
     * Get all messages for a conversation.
     * Validates that the requesting user is a participant or admin.
     */
    public List<ChatMessage> getMessages(Long conversationId, User requestingUser) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // SECURITY: Only buyer, seller, or admin can read
        boolean isParticipant = requestingUser.getId().equals(conversation.getBuyer().getId())
            || requestingUser.getId().equals(conversation.getSeller().getId());
        boolean isAdmin = "ROLE_ADMIN".equals(requestingUser.getRole())
            || "ROLE_SUPER_ADMIN".equals(requestingUser.getRole());

        if (!isParticipant && !isAdmin) {
            throw new SecurityException("You do not have access to this conversation.");
        }

        return chatMessageRepository.findByConversationId(conversationId);
    }

    /**
     * Get conversation for a given auction, if one exists.
     */
    public Optional<Conversation> getConversationByAuction(Auction auction) {
        return conversationRepository.findByAuction(auction);
    }

    /**
     * Delete messages older than 90 days after payment release.
     * Called by scheduled cleanup job.
     */
    @Transactional
    public int cleanupExpiredMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        return chatMessageRepository.deleteExpiredMessages(cutoff);
    }
}
```

---

## Phase 5: Controllers

### 5.1 `ChatController` — HTTP REST endpoints

**File:** `src/main/java/com/auctionhouse/controller/ChatController.java`

```java
@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    // Constructor injection

    /**
     * GET /chat/{conversationId}/messages
     * Returns message history as JSON (for initial load + AJAX refresh fallback)
     */
    @GetMapping("/{conversationId}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByUsername(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatMessage> messages = chatService.getMessages(conversationId, user);

        // Map to DTO (never expose full User entity)
        List<ChatMessageDTO> dtos = messages.stream()
            .map(m -> new ChatMessageDTO(
                m.getId(),
                m.getSender().getUsername(),
                m.getSender().getId().equals(user.getId()), // isMine
                m.getContent(),
                m.getCreatedAt()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
```

### 5.2 `ChatMessageDTO`

**File:** `src/main/java/com/auctionhouse/dto/ChatMessageDTO.java`

```java
public class ChatMessageDTO {
    private Long id;
    private String senderUsername;
    private boolean isMine;
    private String content;
    private LocalDateTime timestamp;

    // Constructor, getters
}
```

### 5.3 `ChatWebSocketController` — STOMP message handler

**File:** `src/main/java/com/auctionhouse/controller/ChatWebSocketController.java`

```java
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // Constructor injection

    /**
     * Handle incoming STOMP messages.
     * Client sends to: /app/chat.send
     * Server broadcasts to: /topic/chat/{conversationId}
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatMessageRequest request,
                               Principal principal) {
        if (principal == null) return; // Not authenticated

        User sender = userService.findByUsername(principal.getName())
            .orElse(null);
        if (sender == null) return;

        try {
            ChatMessage saved = chatService.sendMessage(
                request.getConversationId(),
                sender.getId(),
                request.getContent()
            );

            // Broadcast to all subscribers of this conversation
            ChatMessageDTO dto = new ChatMessageDTO(
                saved.getId(),
                saved.getSender().getUsername(),
                true, // isMine for sender; client determines from username match
                saved.getContent(),
                saved.getCreatedAt()
            );

            messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getConversationId(),
                dto
            );
        } catch (Exception e) {
            // Send error back to sender only
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                new ErrorResponse(e.getMessage())
            );
        }
    }
}
```

### 5.4 `ChatMessageRequest` DTO

**File:** `src/main/java/com/auctionhouse/dto/ChatMessageRequest.java`

```java
public class ChatMessageRequest {
    private Long conversationId;
    private String content;
    // Getters and setters
}
```

---

## Phase 6: Integration with PaymentRelease

### 6.1 Modify `PaymentReleaseService.createPaymentRelease()`

Add conversation creation after saving the PaymentRelease:

```java
@Transactional
public PaymentRelease createPaymentRelease(Auction auction) {
    // ... existing code ...

    PaymentRelease saved = paymentReleaseRepository.save(paymentRelease);

    // AUTO-CREATE CHAT CONVERSATION
    chatService.createConversation(saved);

    // ... existing notifications ...

    return saved;
}
```

### 6.2 Modify `SellerPaymentController.showSubmitForm()`

Pass conversation to the template:

```java
// After loading PaymentRelease:
Optional<Conversation> conversation = chatService.getConversationByAuction(pr.getAuction());
model.addAttribute("conversation", conversation.orElse(null));
```

### 6.3 Modify `BuyerPaymentController.showSubmitForm()`

Same as above — pass conversation to template.

---

## Phase 7: Frontend Templates

### 7.1 Extract reusable chat fragment

**File:** `src/main/resources/templates/fragments/chat-panel.html`

```html
<!-- Chat Panel Fragment — included in seller/buyer submit pages -->
<div th:fragment="chatPanel(conversation)"
     th:if="${conversation != null}"
     class="bg-white rounded-3xl border border-g-border overflow-hidden flex flex-col"
     style="height: 650px;"
     th:attr="data-conversation-id=${conversation.id}">

    <!-- Header -->
    <div class="px-5 py-4 border-b border-g-border flex items-center gap-3 bg-blue-50">
        <span class="material-icons-outlined text-g-blue">chat</span>
        <div class="flex-1">
            <p class="font-medium text-g-text text-sm">
                Chat with <span th:text="${otherPartyUsername}">buyer/seller</span>
            </p>
            <p class="text-xs text-g-text-secondary" id="chatStatus">Connecting...</p>
        </div>
    </div>

    <!-- Messages container -->
    <div id="chatMessages" class="flex-1 overflow-y-auto p-4 space-y-3">
        <div class="text-center py-8">
            <p class="text-xs text-g-text-secondary">Loading messages...</p>
        </div>
    </div>

    <!-- Input -->
    <div class="px-4 py-3 border-t border-g-border">
        <div class="flex items-center gap-2">
            <input type="text" id="chatInput"
                   placeholder="Type a message..."
                   maxlength="2000"
                   class="flex-1 px-3 py-2 bg-g-bg border border-g-border rounded-full text-sm focus:outline-none focus:border-g-blue">
            <button id="chatSendBtn"
                    class="w-9 h-9 bg-g-blue rounded-full flex items-center justify-center text-white hover:bg-g-blue-hover transition-colors disabled:opacity-50"
                    disabled>
                <span class="material-icons-outlined text-lg">send</span>
            </button>
        </div>
    </div>
</div>
```

### 7.2 Chat JavaScript client

**File:** `src/main/resources/static/js/chat-client.js`

```javascript
/**
 * Chat client — connects to WebSocket, loads history, sends messages.
 * Expects a chat panel element with data-conversation-id attribute.
 */
class ChatClient {
    constructor(conversationId, currentUsername) {
        this.conversationId = conversationId;
        this.currentUsername = currentUsername;
        this.stompClient = null;
        this.messagesEl = document.getElementById('chatMessages');
        this.inputEl = document.getElementById('chatInput');
        this.sendBtn = document.getElementById('chatSendBtn');
        this.statusEl = document.getElementById('chatStatus');
    }

    async init() {
        await this.loadHistory();
        this.connectWebSocket();
        this.bindEvents();
    }

    async loadHistory() {
        const response = await fetch(`/chat/${this.conversationId}/messages`);
        if (!response.ok) return;
        const messages = await response.json();
        this.messagesEl.innerHTML = '';
        messages.forEach(msg => this.appendMessage(msg));
        this.scrollToBottom();
    }

    connectWebSocket() {
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // Disable debug logging

        this.stompClient.connect({}, () => {
            this.statusEl.textContent = 'Connected';
            this.sendBtn.disabled = false;

            // Subscribe to conversation channel
            this.stompClient.subscribe(
                `/topic/chat/${this.conversationId}`,
                (message) => {
                    const msg = JSON.parse(message.body);
                    // Update isMine based on current user
                    msg.isMine = (msg.senderUsername === this.currentUsername);
                    this.appendMessage(msg);
                    this.scrollToBottom();
                }
            );
        }, (error) => {
            this.statusEl.textContent = 'Disconnected — reconnecting...';
            setTimeout(() => this.connectWebSocket(), 3000);
        });
    }

    sendMessage() {
        const content = this.inputEl.value.trim();
        if (!content || !this.stompClient?.connected) return;

        this.stompClient.send('/app/chat.send', {},
            JSON.stringify({
                conversationId: this.conversationId,
                content: content
            })
        );
        this.inputEl.value = '';
    }

    appendMessage(msg) {
        const div = document.createElement('div');
        div.className = msg.isMine
            ? 'flex gap-2 max-w-[85%] ml-auto justify-end'
            : 'flex gap-2 max-w-[85%]';

        const bubbleClass = msg.isMine
            ? 'bg-g-blue text-white rounded-2xl rounded-tr-sm'
            : 'bg-g-bg rounded-2xl rounded-tl-sm';
        const textClass = msg.isMine ? 'text-sm' : 'text-sm text-g-text';

        div.innerHTML = `
            <div>
                <div class="${bubbleClass} px-3 py-2">
                    <p class="${textClass}">${this.escapeHtml(msg.content)}</p>
                </div>
                <p class="text-[10px] text-g-text-secondary mt-0.5 ${msg.isMine ? 'mr-1 text-right' : 'ml-1'}">${this.formatTime(msg.timestamp)}</p>
            </div>
        `;
        this.messagesEl.appendChild(div);
    }

    scrollToBottom() {
        this.messagesEl.scrollTop = this.messagesEl.scrollHeight;
    }

    bindEvents() {
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.inputEl.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendMessage();
        });
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    formatTime(timestamp) {
        if (!timestamp) return '';
        const d = new Date(timestamp);
        const now = new Date();
        const diffMs = now - d;
        if (diffMs < 60000) return 'Just now';
        if (diffMs < 3600000) return Math.floor(diffMs / 60000) + 'm ago';
        if (diffMs < 86400000) return Math.floor(diffMs / 3600000) + 'h ago';
        return d.toLocaleDateString();
    }
}
```

### 7.3 Update `seller/submit-details.html`

Add after the existing form:

```html
<!-- In the grid, add chat column -->
<div class="grid lg:grid-cols-2 gap-6">
    <!-- Existing form (left) -->
    <div><!-- ... existing form ... --></div>

    <!-- Chat panel (right) -->
    <div th:replace="~{fragments/chat-panel :: chatPanel(${conversation})}"></div>
</div>

<!-- Add SockJS + STOMP.js + chat client scripts -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
<script src="/js/chat-client.js"></script>
<script th:if="${conversation != null}">
    document.addEventListener('DOMContentLoaded', function() {
        const chat = new ChatClient(
            /*[[${conversation.id}]]*/ 0,
            /*[[${#authentication.name}]]*/ ''
        );
        chat.init();
    });
</script>
```

### 7.4 Update `buyer/submit-details.html`

Same pattern as seller — add chat panel alongside the existing form.

---

## Phase 8: Admin Read-Only View

### 8.1 Update `AdminController.viewPaymentRelease()`

Add conversation messages to model:

```java
// In viewPaymentRelease():
Optional<Conversation> conversation = chatService.getConversationByAuction(pr.getAuction());
if (conversation.isPresent()) {
    List<ChatMessage> chatMessages = chatService.getMessages(conversation.get().getId(), admin);
    model.addAttribute("chatMessages", chatMessages);
    model.addAttribute("conversation", conversation.get());
}
```

### 8.2 Update `admin/payment-release-detail.html`

Add read-only chat section:

```html
<!-- Chat History (Admin Read-Only) -->
<div th:if="${conversation != null}" class="bg-white rounded-2xl border border-g-border p-6 mt-6">
    <h3 class="font-gsans font-medium text-g-text mb-4 flex items-center gap-2">
        <span class="material-icons-outlined text-g-blue">chat</span>
        Chat History
        <span class="text-xs text-g-text-secondary font-normal">(Read-only)</span>
    </h3>
    <div th:if="${chatMessages.isEmpty()}" class="text-center py-8">
        <p class="text-sm text-g-text-secondary">No messages in this conversation.</p>
    </div>
    <div class="space-y-3 max-h-96 overflow-y-auto">
        <div th:each="msg : ${chatMessages}" class="flex gap-2">
            <div class="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-xs font-medium shrink-0"
                 th:text="${#strings.substring(msg.sender.username, 0, 2).toUpperCase()}">AB</div>
            <div>
                <div class="flex items-center gap-2 mb-0.5">
                    <span class="text-xs font-medium text-g-text" th:text="${msg.sender.username}">user</span>
                    <span class="text-[10px] text-g-text-secondary" th:text="${#temporals.format(msg.createdAt, 'MMM d, h:mm a')}">time</span>
                </div>
                <p class="text-sm text-g-text bg-g-bg rounded-lg px-3 py-2" th:text="${msg.content}">message</p>
            </div>
        </div>
    </div>
</div>
```

---

## Phase 9: Scheduled Cleanup

### 9.1 Add cleanup method to `ChatService`

Already defined in Phase 4 — `cleanupExpiredMessages()` deletes messages where payment was released more than 90 days ago.

### 9.2 Add `@Scheduled` method

Either in `ChatService` or a new `ChatCleanupTask`:

```java
@Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
@Transactional
public void cleanupExpiredChatMessages() {
    int deleted = chatService.cleanupExpiredMessages();
    if (deleted > 0) {
        log.info("Cleaned up {} expired chat messages", deleted);
    }
}
```

---

## Phase 10: Implementation Order

Execute in this order to maintain compilability at each step:

| Step | Action | Files |
|------|--------|-------|
| **1** | Add WebSocket dependency | `pom.xml` |
| **2** | Create models | `Conversation.java`, `ChatMessage.java` |
| **3** | Create repositories | `ConversationRepository.java`, `ChatMessageRepository.java` |
| **4** | Create `ChatService` | `ChatService.java` |
| **5** | Create DTOs | `ChatMessageDTO.java`, `ChatMessageRequest.java` |
| **6** | WebSocket config | `WebSocketConfig.java` |
| **7** | Security config updates | `SecurityConfig.java` (add `/ws/**` permit) |
| **8** | HTTP controller | `ChatController.java` |
| **9** | WebSocket controller | `ChatWebSocketController.java` |
| **10** | Integrate with PaymentRelease | `PaymentReleaseService.java` |
| **11** | Update seller/buyer controllers | `SellerPaymentController.java`, `BuyerPaymentController.java` |
| **12** | Create chat fragment | `fragments/chat-panel.html` |
| **13** | Create JS client | `static/js/chat-client.js` |
| **14** | Update seller template | `seller/submit-details.html` |
| **15** | Update buyer template | `buyer/submit-details.html` |
| **16** | Admin view | `AdminController.java`, `admin/payment-release-detail.html` |
| **17** | Scheduled cleanup | Add `@Scheduled` to `ChatService` |
| **18** | Test end-to-end | Manual testing |

---

## Security Checklist (VibeSec)

- [ ] Only buyer/seller can send messages (validated in `ChatService.sendMessage()`)
- [ ] Only buyer/seller/admin can read messages (validated in `ChatService.getMessages()`)
- [ ] Message content HTML-escaped on render (`escapeHtml()` in JS, Thymeleaf auto-escapes)
- [ ] Message length limited to 2000 chars server-side
- [ ] WebSocket connection requires authentication (`Principal` from session)
- [ ] CSRF disabled for WebSocket (uses session cookie), enabled for HTTP endpoints
- [ ] No mass assignment — `ChatMessageRequest` only accepts `conversationId` + `content`
- [ ] Admin view is read-only (no send form rendered for admins)
- [ ] Scheduled cleanup prevents unbounded data growth

---

## Estimated Scope

| Metric | Count |
|--------|-------|
| New Java files | 8 |
| Modified Java files | 5 |
| New template files | 1 (fragment) |
| Modified templates | 3 |
| New JS files | 1 |
| New DB tables | 2 |
| New dependencies | 1 |
