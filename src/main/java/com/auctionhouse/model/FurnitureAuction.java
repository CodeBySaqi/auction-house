package com.auctionhouse.model;

import javax.persistence.*;

/**
 * FurnitureAuction - furniture & home decor specific auction.
 */
@Entity
@DiscriminatorValue("FURNITURE")
public class FurnitureAuction extends Auction {

    @Column(name = "fu_material")
    private String material;

    @Column(name = "fu_dimensions")
    private String dimensions;

    @Column(name = "fu_condition")
    private String condition;

    @Column(name = "fu_style")
    private String style;

    @Column(name = "fu_color")
    private String color;

    public FurnitureAuction() {
        setCategory(AuctionCategory.FURNITURE);
    }

    @Override
    public String getCategoryDetails() {
        return style + " " + material + " piece | " + dimensions + " | " + condition + " | " + color;
    }

    @Override
    public String getTypeName() { return "Furniture"; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
