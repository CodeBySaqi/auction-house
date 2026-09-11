package com.auctionhouse.model;

import javax.persistence.*;

/**
 * CarAuction - specific auction type for vehicles.
 * Demonstrates Inheritance: extends Auction with car-specific properties.
 */
@Entity
@DiscriminatorValue("CAR")
public class CarAuction extends Auction {

    @Column(name = "car_make")
    private String make;

    @Column(name = "car_model")
    private String model;

    @Column(name = "car_year")
    private int year;

    @Column(name = "car_mileage")
    private int mileage;

    @Column(name = "car_condition")
    private String condition;

    @Column(name = "car_color")
    private String color;

    public CarAuction() {
        setCategory(AuctionCategory.CARS);
    }

    @Override
    public String getCategoryDetails() {
        return year + " " + make + " " + model + " | " + String.format("%,d", mileage) + " miles | " + condition + " | " + color;
    }

    @Override
    public String getTypeName() {
        return "Car";
    }

    // Getters and Setters
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
