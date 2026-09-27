package com.auctionhouse.model;

import javax.persistence.*;

/**
 * RealEstateAuction - property/real-estate specific auction.
 */
@Entity
@DiscriminatorValue("REAL_ESTATE")
public class RealEstateAuction extends Auction {

    @Column(name = "re_property_type")
    private String propertyType;

    @Column(name = "re_bedrooms")
    private int bedrooms;

    @Column(name = "re_bathrooms")
    private int bathrooms;

    @Column(name = "re_area_sqft")
    private int areaSqft;

    @Column(name = "re_location")
    private String location;

    @Column(name = "re_year_built")
    private int yearBuilt;

    public RealEstateAuction() {
        setCategory(AuctionCategory.REAL_ESTATE);
    }

    @Override
    public String getCategoryDetails() {
        return propertyType + " | " + bedrooms + " bed / " + bathrooms + " bath | "
                + String.format("%,d", areaSqft) + " sq ft | " + location + " | Built " + yearBuilt;
    }

    @Override
    public String getTypeName() { return "Real Estate"; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public int getBathrooms() { return bathrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }
    public int getAreaSqft() { return areaSqft; }
    public void setAreaSqft(int areaSqft) { this.areaSqft = areaSqft; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(int yearBuilt) { this.yearBuilt = yearBuilt; }
}
