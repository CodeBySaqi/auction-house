package com.auctionhouse.model;

import javax.persistence.*;

/**
 * ArtAuction - specific auction type for fine art pieces.
 * Demonstrates Inheritance: extends Auction with art-specific properties.
 */
@Entity
@DiscriminatorValue("ART")
public class ArtAuction extends Auction {

    @Column(name = "art_artist")
    private String artist;

    @Column(name = "art_medium")
    private String medium;

    @Column(name = "art_year_created")
    private int yearCreated;

    @Column(name = "art_dimensions")
    private String dimensions;

    @Column(name = "art_authenticated")
    private boolean authenticated;

    public ArtAuction() {
        setCategory(AuctionCategory.ART);
    }

    @Override
    public String getCategoryDetails() {
        return "By " + artist + " | " + medium + " | " + yearCreated + " | " + dimensions +
               (authenticated ? " | ✓ Authenticated" : " | Unverified");
    }

    @Override
    public String getTypeName() {
        return "Art";
    }

    // Getters and Setters
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    public int getYearCreated() { return yearCreated; }
    public void setYearCreated(int yearCreated) { this.yearCreated = yearCreated; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
}
