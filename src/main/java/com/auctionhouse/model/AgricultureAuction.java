package com.auctionhouse.model;

import javax.persistence.*;

/**
 * AgricultureAuction - agricultural equipment specific auction.
 */
@Entity
@DiscriminatorValue("AGRICULTURE")
public class AgricultureAuction extends Auction {

    @Column(name = "ag_equipment_type")
    private String equipmentType;

    @Column(name = "ag_brand")
    private String brand;

    @Column(name = "ag_model")
    private String model;

    @Column(name = "ag_year_made")
    private int yearMade;

    @Column(name = "ag_hours_used")
    private int hoursUsed;

    public AgricultureAuction() {
        setCategory(AuctionCategory.AGRICULTURE);
    }

    @Override
    public String getCategoryDetails() {
        return equipmentType + " | " + brand + " " + model + " | Made " + yearMade
                + " | " + String.format("%,d", hoursUsed) + " hours used";
    }

    @Override
    public String getTypeName() { return "Agricultural Equipment"; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYearMade() { return yearMade; }
    public void setYearMade(int yearMade) { this.yearMade = yearMade; }
    public int getHoursUsed() { return hoursUsed; }
    public void setHoursUsed(int hoursUsed) { this.hoursUsed = hoursUsed; }
}
