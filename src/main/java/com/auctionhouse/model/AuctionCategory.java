package com.auctionhouse.model;

/**
 * Enum representing auction categories.
 */
public enum AuctionCategory {
    CARS("Cars & Vehicles"),
    ART("Fine Art"),
    WATCHES("Luxury Watches"),
    JEWELRY("Jewelry & Gems"),
    COLLECTIBLES("Collectibles");

    private final String displayName;

    AuctionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
