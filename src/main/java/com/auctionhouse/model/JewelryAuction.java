package com.auctionhouse.model;

import javax.persistence.*;

/**
 * JewelryAuction - specific auction type for jewelry and gems.
 */
@Entity
@DiscriminatorValue("JEWELRY")
public class JewelryAuction extends Auction {

    @Column(name = "jewelry_metal")
    private String metalType;

    @Column(name = "jewelry_gemstone")
    private String gemstone;

    @Column(name = "jewelry_carat")
    private double carat;

    @Column(name = "jewelry_designer")
    private String designer;

    @Column(name = "jewelry_certified")
    private boolean certified;

    public JewelryAuction() {
        setCategory(AuctionCategory.JEWELRY);
    }

    @Override
    public String getCategoryDetails() {
        return metalType + " | " + gemstone + " (" + carat + " ct) | By " + designer +
               (certified ? " | ✓ GIA Certified" : "");
    }

    @Override
    public String getTypeName() {
        return "Jewelry";
    }

    public String getMetalType() { return metalType; }
    public void setMetalType(String metalType) { this.metalType = metalType; }

    public String getGemstone() { return gemstone; }
    public void setGemstone(String gemstone) { this.gemstone = gemstone; }

    public double getCarat() { return carat; }
    public void setCarat(double carat) { this.carat = carat; }

    public String getDesigner() { return designer; }
    public void setDesigner(String designer) { this.designer = designer; }

    public boolean isCertified() { return certified; }
    public void setCertified(boolean certified) { this.certified = certified; }
}
