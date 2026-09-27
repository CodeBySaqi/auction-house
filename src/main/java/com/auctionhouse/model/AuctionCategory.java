package com.auctionhouse.model;

/**
 * Enum representing auction categories.
 */
public enum AuctionCategory {
    CARS("Cars & Vehicles"),
    ART("Fine Art"),
    WATCHES("Luxury Watches"),
    JEWELRY("Jewelry & Gems"),
    COLLECTIBLES("Collectibles"),
    REAL_ESTATE("Real Estate & Property"),
    ELECTRONICS("Electronics & Gadgets"),
    FURNITURE("Furniture & Home"),
    INSTRUMENTS("Musical Instruments"),
    SPORTS("Sports & Fitness"),
    INDUSTRIAL("Industrial & Machinery"),
    AGRICULTURE("Agricultural Equipment"),
    LIVESTOCK("Livestock & Animals"),
    SOLAR_POWER("Solar & Power Equipment");

    private final String displayName;

    AuctionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
