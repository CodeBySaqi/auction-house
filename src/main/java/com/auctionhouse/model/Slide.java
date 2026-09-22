package com.auctionhouse.model;

import javax.persistence.*;

/**
 * Slide entity - represents a hero slider slide on the homepage.
 * Managed via admin dashboard.
 */
@Entity
@Table(name = "slides")
public class Slide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String subtitle;

    @Column(name = "button_text")
    private String buttonText;

    @Column(name = "button_url")
    private String buttonUrl;

    @Column(name = "button_color")
    private String buttonColor = "#3b82f6";

    @Column(name = "background_color")
    private String backgroundColor = "#ffffff";

    @Column(name = "background_gradient")
    private String backgroundGradient;

    @Column(name = "text_color")
    private String textColor = "#1f2937";

    @Column(name = "subtitle_color")
    private String subtitleColor = "#4b5563";

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "image_shape")
    private String imageShape = "rounded-[40%_60%_70%_30%/40%_50%_60%_50%]";

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    public Slide() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getButtonText() { return buttonText; }
    public void setButtonText(String buttonText) { this.buttonText = buttonText; }

    public String getButtonUrl() { return buttonUrl; }
    public void setButtonUrl(String buttonUrl) { this.buttonUrl = buttonUrl; }

    public String getButtonColor() { return buttonColor; }
    public void setButtonColor(String buttonColor) { this.buttonColor = buttonColor; }

    public String getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

    public String getBackgroundGradient() { return backgroundGradient; }
    public void setBackgroundGradient(String backgroundGradient) { this.backgroundGradient = backgroundGradient; }

    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }

    public String getSubtitleColor() { return subtitleColor; }
    public void setSubtitleColor(String subtitleColor) { this.subtitleColor = subtitleColor; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageShape() { return imageShape; }
    public void setImageShape(String imageShape) { this.imageShape = imageShape; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
