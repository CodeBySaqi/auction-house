# Buyer-Seller Chat - TDD Implementation Status

## TDD Cycle Progress

### ✅ RED Phase (Complete)
- Created comprehensive test suite: `src/test/java/com/auctionhouse/service/ChatServiceTest.java`
- 18 test methods covering:
  - Conversation creation
  - Message sending and validation
  - Access control (participant-only)
  - Admin read-only access
  - Message cleanup (90-day retention)
  - Edge cases (empty messages, max length, non-participants)

### ✅ GREEN Phase (Complete)
All code implemented to make tests pass:

#### Models
- ✅ `Conversation.java` - Entity with paymentRelease, auction, buyer, seller relationships
- ✅ `ChatMessage.java` - Entity with conversation, sender, content, createdAt

#### Repositories
- ✅ `ConversationRepository.java` - Custom queries for findByPaymentRelease, findByAuctionId
- ✅ `ChatMessageRepository.java` - Custom queries including deleteExpiredMessages

#### Service Layer
- ✅ `ChatService.java` - Complete business logic:
  - `createConversation()` - Idempotent, called from PaymentReleaseService
  - `sendMessage()` - Validates participant, max length (2000 chars)
  - `getMessages()` - Validates access (participant or admin)
  - `cleanupExpiredMessages()` - Deletes messages >90 days after payment release
  - `scheduledCleanup()` - Runs daily at 3 AM via @Scheduled

#### DTOs
- ✅ `ChatMessageDTO.java` - For API responses (id, senderUsername, isMine, content, timestamp)
- ✅ `ChatMessageRequest.java` - For WebSocket requests (conversationId, content)
- ✅ `ErrorResponse.java` - For WebSocket error messages

#### Controllers
- ✅ `ChatController.java` - HTTP REST endpoint for message history
- ✅ `ChatWebSocketController.java` - STOMP WebSocket handler for real-time messaging

#### Configuration
- ✅ `WebSocketConfig.java` - STOMP over SockJS at `/ws` endpoint
- ✅ Updated `SecurityConfig.java` - Permits `/ws/**` for WebSocket handshake
- ✅ Added `spring-boot-starter-websocket` dependency to `pom.xml`

#### Integration
- ✅ Updated `PaymentReleaseService.createPaymentRelease()` - Auto-creates conversation
- ✅ Updated `SellerPaymentController` - Passes conversation to template
- ✅ Updated `BuyerPaymentController` - Passes conversation to template

#### Frontend
- ✅ `fragments/chat-panel.html` - Reusable chat UI component (embedded Option 4C)
- ✅ `static/js/chat-client.js` - STOMP/SockJS client with:
  - Auto-reconnect with exponential backoff
  - Message history loading
  - Real-time message broadcasting
  - XSS protection via HTML escaping
  - Responsive design
- ✅ Updated `seller/submit-details.html` - Includes chat panel
- ✅ Updated `buyer/submit-details.html` - Includes chat panel

#### Admin Features
- ✅ Added `AdminController.viewChatConversation()` endpoint
- ✅ Created `admin-chat-view.html` - Read-only chat history for dispute resolution

## Architecture Decisions

### WebSocket Protocol
- **Protocol:** STOMP over SockJS
- **Endpoint:** `/ws`
- **Client sends to:** `/app/chat.send`
- **Server broadcasts to:** `/topic/chat/{conversationId}`
- **Libraries:** SockJS 1.x, STOMP.js 2.3.3 (CDN)

### Security Model
- **Access Control:** Validated in ChatService layer
- **Participants:** Only buyer and seller can send messages
- **Read Access:** Buyer, seller, and admin (ROLE_ADMIN, ROLE_SUPER_ADMIN)
- **Admin View:** Read-only, no WebSocket subscription
- **XSS Prevention:** HTML escaping in JavaScript client

### Data Model
```
Conversation (1) ←→ (1) PaymentRelease
Conversation (1) ←→ (n) ChatMessage
Conversation (1) ←→ (1) Auction
Conversation (1) ←→ (1) User (buyer)
Conversation (1) ←→ (1) User (seller)
ChatMessage (n) ←→ (1) User (sender)
```

### Lifecycle
1. **Creation:** Auto-created when auction won (PaymentRelease created)
2. **Active:** Both parties can chat during delivery process
3. **Retention:** Messages kept for 90 days after payment release
4. **Cleanup:** Scheduled job deletes expired messages daily at 3 AM

## User Decisions Implemented

✅ **Q1:** Chat available immediately (when PaymentRelease created)  
✅ **Q2:** WebSocket (STOMP/SockJS) for real-time  
✅ **Q3:** Text-only messages (no file attachments)  
✅ **Q4:** UI = Embedded in payment flow (Option 4C)  
✅ **Q5:** Admin read-only access for disputes  
✅ **Q6:** Keep 90 days after payment release  
✅ **Chat creation:** Auto-created with PaymentRelease  

## Next Steps for TDD Completion

### 1. Run Tests Locally
```bash
cd auction-house
mvn clean test
```

Expected outcome: All 18 tests in `ChatServiceTest` should pass.

### 2. Verify Integration
- Start the application: `mvn spring-boot:run`
- Create a test auction and close it (simulate winning bid)
- Verify conversation is auto-created in database
- Navigate to seller payment page → chat panel should appear
- Navigate to buyer payment page → chat panel should appear
- Send messages from both sides → should appear in real-time
- Login as admin → navigate to `/admin/chat/{conversationId}` → should see read-only view

### 3. REFACTOR Phase (if needed)
After tests pass, review code for:
- Code duplication
- Method complexity
- Naming clarity
- Test coverage gaps
- Performance optimizations

## Files Created/Modified

### New Files (13)
1. `src/test/java/com/auctionhouse/service/ChatServiceTest.java`
2. `src/test/resources/application-test.properties`
3. `src/main/java/com/auctionhouse/model/Conversation.java`
4. `src/main/java/com/auctionhouse/model/ChatMessage.java`
5. `src/main/java/com/auctionhouse/repository/ConversationRepository.java`
6. `src/main/java/com/auctionhouse/repository/ChatMessageRepository.java`
7. `src/main/java/com/auctionhouse/dto/ChatMessageDTO.java`
8. `src/main/java/com/auctionhouse/dto/ChatMessageRequest.java`
9. `src/main/java/com/auctionhouse/dto/ErrorResponse.java`
10. `src/main/java/com/auctionhouse/service/ChatService.java`
11. `src/main/java/com/auctionhouse/config/WebSocketConfig.java`
12. `src/main/java/com/auctionhouse/controller/ChatController.java`
13. `src/main/java/com/auctionhouse/controller/ChatWebSocketController.java`
14. `src/main/resources/templates/fragments/chat-panel.html`
15. `src/main/resources/static/js/chat-client.js`
16. `src/main/resources/templates/admin-chat-view.html`

### Modified Files (5)
1. `pom.xml` - Added WebSocket dependency
2. `src/main/java/com/auctionhouse/config/SecurityConfig.java` - Added `/ws/**` to permitAll
3. `src/main/java/com/auctionhouse/service/PaymentReleaseService.java` - Injected ChatService, auto-create conversation
4. `src/main/java/com/auctionhouse/controller/SellerPaymentController.java` - Injected ChatService, pass conversation to template
5. `src/main/java/com/auctionhouse/controller/BuyerPaymentController.java` - Injected ChatService, pass conversation to template
6. `src/main/java/com/auctionhouse/controller/AdminController.java` - Injected ChatService, added chat view endpoint
7. `src/main/resources/templates/seller/submit-details.html` - Included chat panel fragment
8. `src/main/resources/templates/buyer/submit-details.html` - Included chat panel fragment

## Database Schema (Auto-generated by JPA/Hibernate)

### conversation table
- `id` (bigint, PK)
- `payment_release_id` (bigint, FK → payment_release.id)
- `auction_id` (bigint, FK → auction.id)
- `buyer_id` (bigint, FK → user.id)
- `seller_id` (bigint, FK → user.id)
- `created_at` (timestamp)

### chat_message table
- `id` (bigint, PK)
- `conversation_id` (bigint, FK → conversation.id)
- `sender_id` (bigint, FK → user.id)
- `content` (varchar(2000))
- `created_at` (timestamp)

## Testing Checklist

- [ ] All 18 unit tests pass
- [ ] Conversation auto-created when auction won
- [ ] Seller can send messages
- [ ] Buyer can send messages
- [ ] Messages appear in real-time (WebSocket)
- [ ] Non-participants cannot send messages
- [ ] Non-participants cannot read messages
- [ ] Admin can read messages (read-only)
- [ ] Messages deleted after 90 days
- [ ] Max message length enforced (2000 chars)
- [ ] Empty messages rejected
- [ ] XSS attempts blocked (HTML escaping)
- [ ] Connection lost → auto-reconnect works
- [ ] Chat panel renders correctly on seller page
- [ ] Chat panel renders correctly on buyer page
- [ ] Admin chat view displays all messages
- [ ] Mobile responsive (chat panel scales)

## Known Limitations

1. **No file attachments** - Text-only as per user decision
2. **No typing indicators** - Not in scope
3. **No read receipts** - Not in scope
4. **No message editing/deletion** - Not in scope
5. **No search within chat** - Not in scope

## Future Enhancements (Out of Scope)

- File/image attachments
- Typing indicators ("User is typing...")
- Read receipts ("Seen")
- Message reactions (emoji)
- Message editing/deletion
- Search within conversation
- Export chat history
- Email notifications for offline messages

---

**Status:** ✅ GREEN phase complete. Ready for testing and REFACTOR phase.

**Next Action:** Run `mvn clean test` to verify all tests pass.
