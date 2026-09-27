package com.auctionhouse.model;

import javax.persistence.*;

/**
 * LivestockAuction - livestock & animals specific auction.
 */
@Entity
@DiscriminatorValue("LIVESTOCK")
public class LivestockAuction extends Auction {

    @Column(name = "lv_animal_type")
    private String animalType;

    @Column(name = "lv_breed")
    private String breed;

    @Column(name = "lv_age_months")
    private int ageMonths;

    @Column(name = "lv_weight_kg")
    private int weightKg;

    @Column(name = "lv_health_status")
    private String healthStatus;

    @Column(name = "lv_vaccinated")
    private boolean vaccinated;

    public LivestockAuction() {
        setCategory(AuctionCategory.LIVESTOCK);
    }

    @Override
    public String getCategoryDetails() {
        return animalType + " (" + breed + ") | " + ageMonths + " months old | " + weightKg + " kg | "
                + healthStatus + " | " + (vaccinated ? "Vaccinated" : "Not vaccinated");
    }

    @Override
    public String getTypeName() { return "Livestock"; }

    public String getAnimalType() { return animalType; }
    public void setAnimalType(String animalType) { this.animalType = animalType; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public int getAgeMonths() { return ageMonths; }
    public void setAgeMonths(int ageMonths) { this.ageMonths = ageMonths; }
    public int getWeightKg() { return weightKg; }
    public void setWeightKg(int weightKg) { this.weightKg = weightKg; }
    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }
    public boolean isVaccinated() { return vaccinated; }
    public void setVaccinated(boolean vaccinated) { this.vaccinated = vaccinated; }
}
