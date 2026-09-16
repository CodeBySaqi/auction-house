package com.auctionhouse.service;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for ChatService - Write failing tests first, then implement.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PaymentReleaseRepository paymentReleaseRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    private User buyer;
    private User seller;
    private User admin;
    private User outsider;
    private Auction auction;
    private PaymentRelease paymentRelease;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        paymentReleaseRepository.deleteAll();
        auctionRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        buyer = new User();
        buyer.setUsername("buyer123");
        buyer.setEmail("buyer@test.com");
        buyer.setPassword("password");
        buyer.setRole("ROLE_USER");
        buyer = userRepository.save(buyer);

        seller = new User();
        seller.setUsername("seller456");
        seller.setEmail("seller@test.com");
        seller.setPassword("password");
        seller.setRole("ROLE_USER");
        seller = userRepository.save(seller);

        admin = new User();
        admin.setUsername("admin789");
        admin.setEmail("admin@test.com");
        admin.setPassword("password");
        admin.setRole("ROLE_ADMIN");
        admin = userRepository.save(admin);

        outsider = new User();
        outsider.setUsername("outsider");
        outsider.setEmail("outsider@test.com");
        outsider.setPassword("password");
        outsider.setRole("ROLE_USER");
        outsider = userRepository.save(outsider);

        // Create test auction (Auction is abstract, use CarAuction)
        CarAuction carAuction = new CarAuction();
        carAuction.setTitle("Test Auction");
        carAuction.setDescription("Test description");
        carAuction.setStartingPrice(100.0);
        carAuction.setCurrentHighestBid(150.0);
        carAuction.setEndTime(LocalDateTime.now().plusDays(1));
        carAuction.setStatus(AuctionStatus.CLOSED);
        carAuction.setHighestBidder(buyer);
        carAuction.setCreatedBy(seller);
        carAuction.setImageUrl("http://example.com/image.jpg");
        carAuction.setCategory(AuctionCategory.CARS);
        carAuction.setMake("Toyota");
        carAuction.setModel("Camry");
        carAuction.setYear(2020);
        carAuction.setMileage(50000);
        carAuction.setCondition("Good");
        carAuction.setColor("Blue");
        auction = auctionRepository.save(carAuction);

        // Create test payment release
        paymentRelease = new PaymentRelease();
        paymentRelease.setAuction(auction);
        paymentRelease.setBuyer(buyer);
        paymentRelease.setSeller(seller);
        paymentRelease.setWinningAmount(BigDecimal.valueOf(150.0));
        paymentRelease.setStatus(VerificationStatus.WAITING_FOR_DETAILS);
        paymentRelease.setCreatedAt(LocalDateTime.now());
        paymentRelease = paymentReleaseRepository.save(paymentRelease);
    }

    @Test
    void createConversation_shouldCreateNewConversation() {
        // WHEN
        Conversation conversation = chatService.createConversation(paymentRelease);

        // THEN
        assertNotNull(conversation);
        assertNotNull(conversation.getId());
        assertEquals(paymentRelease.getId(), conversation.getPaymentRelease().getId());
        assertEquals(auction.getId(), conversation.getAuction().getId());
        assertEquals(buyer.getId(), conversation.getBuyer().getId());
        assertEquals(seller.getId(), conversation.getSeller().getId());
        assertNotNull(conversation.getCreatedAt());
    }

    @Test
    void createConversation_shouldBeIdempotent() {
        // GIVEN - first creation
        Conversation first = chatService.createConversation(paymentRelease);

        // WHEN - second creation with same payment release
        Conversation second = chatService.createConversation(paymentRelease);

        // THEN - should return same conversation
        assertEquals(first.getId(), second.getId());
        assertEquals(1, conversationRepository.count());
    }

    @Test
    void sendMessage_shouldSaveMessageWhenSenderIsBuyer() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        String content = "Hello seller!";

        // WHEN
        ChatMessage message = chatService.sendMessage(conversation.getId(), buyer.getId(), content);

        // THEN
        assertNotNull(message);
        assertNotNull(message.getId());
        assertEquals(content, message.getContent());
        assertEquals(buyer.getId(), message.getSender().getId());
        assertEquals(conversation.getId(), message.getConversation().getId());
        assertNotNull(message.getCreatedAt());
    }

    @Test
    void sendMessage_shouldSaveMessageWhenSenderIsSeller() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        String content = "Hello buyer!";

        // WHEN
        ChatMessage message = chatService.sendMessage(conversation.getId(), seller.getId(), content);

        // THEN
        assertNotNull(message);
        assertEquals(content, message.getContent());
        assertEquals(seller.getId(), message.getSender().getId());
    }

    @Test
    void sendMessage_shouldThrowWhenSenderIsNotParticipant() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);

        // WHEN/THEN
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            chatService.sendMessage(conversation.getId(), outsider.getId(), "Unauthorized message");
        });

        assertTrue(exception.getMessage().contains("not a participant"));
    }

    @Test
    void sendMessage_shouldThrowWhenContentIsEmpty() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);

        // WHEN/THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(conversation.getId(), buyer.getId(), "");
        });

        assertTrue(exception.getMessage().contains("cannot be empty"));
    }

    @Test
    void sendMessage_shouldThrowWhenContentIsNull() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);

        // WHEN/THEN
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(conversation.getId(), buyer.getId(), null);
        });
    }

    @Test
    void sendMessage_shouldThrowWhenContentExceedsMaxLength() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        String longContent = "a".repeat(2001); // 2001 chars, max is 2000

        // WHEN/THEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(conversation.getId(), buyer.getId(), longContent);
        });

        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    void sendMessage_shouldTrimWhitespace() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        String content = "  Hello with spaces  ";

        // WHEN
        ChatMessage message = chatService.sendMessage(conversation.getId(), buyer.getId(), content);

        // THEN
        assertEquals("Hello with spaces", message.getContent());
    }

    @Test
    void getMessages_shouldReturnMessagesForBuyer() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        chatService.sendMessage(conversation.getId(), buyer.getId(), "Message 1");
        chatService.sendMessage(conversation.getId(), seller.getId(), "Message 2");

        // WHEN
        List<ChatMessage> messages = chatService.getMessages(conversation.getId(), buyer);

        // THEN
        assertEquals(2, messages.size());
        assertEquals("Message 1", messages.get(0).getContent());
        assertEquals("Message 2", messages.get(1).getContent());
    }

    @Test
    void getMessages_shouldReturnMessagesForSeller() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        chatService.sendMessage(conversation.getId(), buyer.getId(), "Hello");

        // WHEN
        List<ChatMessage> messages = chatService.getMessages(conversation.getId(), seller);

        // THEN
        assertEquals(1, messages.size());
    }

    @Test
    void getMessages_shouldReturnMessagesForAdmin() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);
        chatService.sendMessage(conversation.getId(), buyer.getId(), "Hello");

        // WHEN
        List<ChatMessage> messages = chatService.getMessages(conversation.getId(), admin);

        // THEN
        assertEquals(1, messages.size());
    }

    @Test
    void getMessages_shouldThrowWhenUserIsNotParticipantOrAdmin() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);

        // WHEN/THEN
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            chatService.getMessages(conversation.getId(), outsider);
        });

        assertTrue(exception.getMessage().contains("do not have access"));
    }

    @Test
    void getMessages_shouldReturnEmptyListWhenNoMessages() {
        // GIVEN
        Conversation conversation = chatService.createConversation(paymentRelease);

        // WHEN
        List<ChatMessage> messages = chatService.getMessages(conversation.getId(), buyer);

        // THEN
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void cleanupExpiredMessages_shouldDeleteOldMessages() {
        // GIVEN - Create conversation with old payment release
        PaymentRelease oldRelease = new PaymentRelease();
        oldRelease.setAuction(auction);
        oldRelease.setBuyer(buyer);
        oldRelease.setSeller(seller);
        oldRelease.setWinningAmount(BigDecimal.valueOf(150.0));
        oldRelease.setStatus(VerificationStatus.PAYMENT_RELEASED);
        oldRelease.setPaymentReleased(true);
        oldRelease.setReleasedAt(LocalDateTime.now().minusDays(91)); // 91 days ago
        oldRelease = paymentReleaseRepository.save(oldRelease);

        Conversation conversation = chatService.createConversation(oldRelease);
        chatService.sendMessage(conversation.getId(), buyer.getId(), "Old message");

        // WHEN
        int deleted = chatService.cleanupExpiredMessages();

        // THEN
        assertEquals(1, deleted);
        List<ChatMessage> remaining = chatMessageRepository.findByConversationId(conversation.getId());
        assertTrue(remaining.isEmpty());
    }

    @Test
    void cleanupExpiredMessages_shouldNotDeleteRecentMessages() {
        // GIVEN - Create conversation with recent payment release
        PaymentRelease recentRelease = new PaymentRelease();
        recentRelease.setAuction(auction);
        recentRelease.setBuyer(buyer);
        recentRelease.setSeller(seller);
        recentRelease.setWinningAmount(BigDecimal.valueOf(150.0));
        recentRelease.setStatus(VerificationStatus.PAYMENT_RELEASED);
        recentRelease.setPaymentReleased(true);
        recentRelease.setReleasedAt(LocalDateTime.now().minusDays(89)); // 89 days ago
        recentRelease = paymentReleaseRepository.save(recentRelease);

        Conversation conversation = chatService.createConversation(recentRelease);
        chatService.sendMessage(conversation.getId(), buyer.getId(), "Recent message");

        // WHEN
        int deleted = chatService.cleanupExpiredMessages();

        // THEN
        assertEquals(0, deleted);
        List<ChatMessage> remaining = chatMessageRepository.findByConversationId(conversation.getId());
        assertEquals(1, remaining.size());
    }

    @Test
    void cleanupExpiredMessages_shouldNotDeleteUnreleasedConversations() {
        // GIVEN - conversation with no payment release yet
        Conversation conversation = chatService.createConversation(paymentRelease);
        chatService.sendMessage(conversation.getId(), buyer.getId(), "Pending message");

        // WHEN
        int deleted = chatService.cleanupExpiredMessages();

        // THEN
        assertEquals(0, deleted);
        List<ChatMessage> remaining = chatMessageRepository.findByConversationId(conversation.getId());
        assertEquals(1, remaining.size());
    }
}
