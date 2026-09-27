package com.auctionhouse.model;

import javax.persistence.*;

/**
 * AuctionImage - one photo of an auction. An auction can have multiple
 * photos; the first (by sortOrder) is mirrored into Auction.imageUrl so
 * all existing card listings keep working unchanged.
 */
@Entity
@Table(name = "auction_images")
public class AuctionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public AuctionImage() {}

    public AuctionImage(Auction auction, String url, int sortOrder) {
        this.auction = auction;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
