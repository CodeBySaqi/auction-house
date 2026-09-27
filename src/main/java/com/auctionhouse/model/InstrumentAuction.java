package com.auctionhouse.model;

import javax.persistence.*;

/**
 * InstrumentAuction - musical instruments specific auction.
 */
@Entity
@DiscriminatorValue("INSTRUMENTS")
public class InstrumentAuction extends Auction {

    @Column(name = "in_instrument_type")
    private String instrumentType;

    @Column(name = "in_brand")
    private String brand;

    @Column(name = "in_model")
    private String model;

    @Column(name = "in_year_made")
    private int yearMade;

    @Column(name = "in_condition")
    private String condition;

    public InstrumentAuction() {
        setCategory(AuctionCategory.INSTRUMENTS);
    }

    @Override
    public String getCategoryDetails() {
        return instrumentType + " | " + brand + " " + model + " | Made " + yearMade + " | " + condition;
    }

    @Override
    public String getTypeName() { return "Musical Instrument"; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYearMade() { return yearMade; }
    public void setYearMade(int yearMade) { this.yearMade = yearMade; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
