package com.auctionhouse.model;

import javax.persistence.*;

/**
 * ElectronicsAuction - electronics & gadgets specific auction.
 */
@Entity
@DiscriminatorValue("ELECTRONICS")
public class ElectronicsAuction extends Auction {

    @Column(name = "el_brand")
    private String brand;

    @Column(name = "el_model")
    private String model;

    @Column(name = "el_condition")
    private String condition;

    @Column(name = "el_warranty_months")
    private int warrantyMonths;

    @Column(name = "el_included_items")
    private String includedItems;

    public ElectronicsAuction() {
        setCategory(AuctionCategory.ELECTRONICS);
    }

    @Override
    public String getCategoryDetails() {
        return brand + " " + model + " | " + condition
                + " | Warranty: " + (warrantyMonths > 0 ? warrantyMonths + " months" : "None")
                + (includedItems != null && !includedItems.isEmpty() ? " | Includes: " + includedItems : "");
    }

    @Override
    public String getTypeName() { return "Electronics"; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getIncludedItems() { return includedItems; }
    public void setIncludedItems(String includedItems) { this.includedItems = includedItems; }
}
