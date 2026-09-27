package com.auctionhouse.model;

import javax.persistence.*;

/**
 * SolarPowerAuction - solar & power equipment specific auction
 * (solar panels, generators, UPS, batteries, inverters).
 */
@Entity
@DiscriminatorValue("SOLAR_POWER")
public class SolarPowerAuction extends Auction {

    @Column(name = "sl_item_type")
    private String itemType;

    @Column(name = "sl_capacity_watts")
    private int capacityWatts;

    @Column(name = "sl_brand")
    private String brand;

    @Column(name = "sl_condition")
    private String condition;

    @Column(name = "sl_included_items")
    private String includedItems;

    public SolarPowerAuction() {
        setCategory(AuctionCategory.SOLAR_POWER);
    }

    @Override
    public String getCategoryDetails() {
        return itemType + " | " + brand + " | "
                + (capacityWatts > 0 ? String.format("%,d", capacityWatts) + " W | " : "")
                + condition
                + (includedItems != null && !includedItems.isEmpty() ? " | Includes: " + includedItems : "");
    }

    @Override
    public String getTypeName() { return "Solar & Power"; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public int getCapacityWatts() { return capacityWatts; }
    public void setCapacityWatts(int capacityWatts) { this.capacityWatts = capacityWatts; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getIncludedItems() { return includedItems; }
    public void setIncludedItems(String includedItems) { this.includedItems = includedItems; }
}
