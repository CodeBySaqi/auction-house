package com.auctionhouse.model;

import javax.persistence.*;

/**
 * CollectibleAuction - specific auction type for rare collectibles.
 */
@Entity
@DiscriminatorValue("COLLECTIBLE")
public class CollectibleAuction extends Auction {

    @Column(name = "col_subcategory")
    private String subcategory;

    @Column(name = "col_era")
    private String era;

    @Column(name = "col_condition")
    private String condition;

    @Column(name = "col_rarity")
    private String rarity;

    @Column(name = "col_provenance")
    private String provenance;

    public CollectibleAuction() {
        setCategory(AuctionCategory.COLLECTIBLES);
    }

    @Override
    public String getCategoryDetails() {
        return subcategory + " | Era: " + era + " | " + condition + " | Rarity: " + rarity;
    }

    @Override
    public String getTypeName() {
        return "Collectible";
    }

    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }

    public String getEra() { return era; }
    public void setEra(String era) { this.era = era; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
}
