package com.auctionhouse.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation entity - represents a chat conversation between two users.
 * Can be either:
 *   - An auction-linked conversation (buyer/seller after auction won)
 *   - A direct message conversation (any two users)
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_release_id", unique = true)
    private PaymentRelease paymentRelease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "is_direct_message", nullable = false, columnDefinition = "boolean default false")
    private boolean directMessage = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    public Conversation() {
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = LocalDateTime.now();
    }

    /**
     * Get the other participant in this conversation (not the given user).
     */
    public User getOtherParticipant(User currentUser) {
        if (buyer != null && buyer.getId().equals(currentUser.getId())) {
            return seller;
        }
        return buyer;
    }

    /**
     * Get a display title for the conversation.
     */
    public String getDisplayTitle(User currentUser) {
        User other = getOtherParticipant(currentUser);
        if (directMessage) {
            return other != null ? other.getUsername() : "Unknown";
        }
        if (auction != null) {
            return auction.getTitle();
        }
        return other != null ? other.getUsername() : "Conversation";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PaymentRelease getPaymentRelease() { return paymentRelease; }
    public void setPaymentRelease(PaymentRelease paymentRelease) { this.paymentRelease = paymentRelease; }

    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public boolean isDirectMessage() { return directMessage; }
    public void setDirectMessage(boolean directMessage) { this.directMessage = directMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
}
