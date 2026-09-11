package com.auctionhouse.model;

import javax.persistence.*;

/**
 * WatchAuction - specific auction type for luxury watches.
 */
@Entity
@DiscriminatorValue("WATCH")
public class WatchAuction extends Auction {

    @Column(name = "watch_brand")
    private String brand;

    @Column(name = "watch_model_name")
    private String modelName;

    @Column(name = "watch_movement")
    private String movement;

    @Column(name = "watch_case_material")
    private String caseMaterial;

    @Column(name = "watch_reference")
    private String referenceNumber;

    public WatchAuction() {
        setCategory(AuctionCategory.WATCHES);
    }

    @Override
    public String getCategoryDetails() {
        return brand + " " + modelName + " | Ref: " + referenceNumber + " | " + movement + " | " + caseMaterial;
    }

    @Override
    public String getTypeName() {
        return "Watch";
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getMovement() { return movement; }
    public void setMovement(String movement) { this.movement = movement; }

    public String getCaseMaterial() { return caseMaterial; }
    public void setCaseMaterial(String caseMaterial) { this.caseMaterial = caseMaterial; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
