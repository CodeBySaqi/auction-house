package com.auctionhouse.model;

import javax.persistence.*;

/**
 * IndustrialAuction - industrial machinery specific auction.
 */
@Entity
@DiscriminatorValue("INDUSTRIAL")
public class IndustrialAuction extends Auction {

    @Column(name = "id_machine_type")
    private String machineType;

    @Column(name = "id_manufacturer")
    private String manufacturer;

    @Column(name = "id_model")
    private String model;

    @Column(name = "id_year_made")
    private int yearMade;

    @Column(name = "id_hours_used")
    private int hoursUsed;

    @Column(name = "id_power_source")
    private String powerSource;

    public IndustrialAuction() {
        setCategory(AuctionCategory.INDUSTRIAL);
    }

    @Override
    public String getCategoryDetails() {
        return machineType + " | " + manufacturer + " " + model + " | Made " + yearMade
                + " | " + String.format("%,d", hoursUsed) + " hours used | Power: " + powerSource;
    }

    @Override
    public String getTypeName() { return "Industrial Machinery"; }

    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYearMade() { return yearMade; }
    public void setYearMade(int yearMade) { this.yearMade = yearMade; }
    public int getHoursUsed() { return hoursUsed; }
    public void setHoursUsed(int hoursUsed) { this.hoursUsed = hoursUsed; }
    public String getPowerSource() { return powerSource; }
    public void setPowerSource(String powerSource) { this.powerSource = powerSource; }
}
