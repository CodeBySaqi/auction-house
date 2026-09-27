package com.auctionhouse.model;

import javax.persistence.*;

/**
 * SportsAuction - sports & fitness equipment specific auction.
 */
@Entity
@DiscriminatorValue("SPORTS")
public class SportsAuction extends Auction {

    @Column(name = "sp_equipment_type")
    private String equipmentType;

    @Column(name = "sp_brand")
    private String brand;

    @Column(name = "sp_condition")
    private String condition;

    @Column(name = "sp_size")
    private String sizeSpec;

    @Column(name = "sp_included_items")
    private String includedItems;

    public SportsAuction() {
        setCategory(AuctionCategory.SPORTS);
    }

    @Override
    public String getCategoryDetails() {
        return equipmentType + " | " + brand + " | " + condition
                + (sizeSpec != null && !sizeSpec.isEmpty() ? " | Size: " + sizeSpec : "")
                + (includedItems != null && !includedItems.isEmpty() ? " | Includes: " + includedItems : "");
    }

    @Override
    public String getTypeName() { return "Sports & Fitness"; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getSizeSpec() { return sizeSpec; }
    public void setSizeSpec(String sizeSpec) { this.sizeSpec = sizeSpec; }
    public String getIncludedItems() { return includedItems; }
    public void setIncludedItems(String includedItems) { this.includedItems = includedItems; }
}
