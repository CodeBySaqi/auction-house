package com.auctionhouse.config;

import com.auctionhouse.model.Slide;
import com.auctionhouse.repository.SlideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds the hero slider with the original 3 slides if none exist.
 */
@Component
public class SlideDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(SlideDataSeeder.class);

    private final SlideRepository slideRepository;

    @Autowired
    public SlideDataSeeder(SlideRepository slideRepository) {
        this.slideRepository = slideRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedSlides() {
        if (slideRepository.count() > 0) {
            return; // Already seeded
        }

        log.info("Seeding hero slider with default slides...");

        // Slide 1: Welcome
        Slide s1 = new Slide();
        s1.setTitle("Welcome to AuctionHouse");
        s1.setSubtitle("Bid on luxury cars, watches, art & more");
        s1.setButtonText("Browse Auctions");
        s1.setButtonUrl("/auctions");
        s1.setButtonColor("#3b82f6");
        s1.setBackgroundColor("#ffffff");
        s1.setBackgroundGradient(null);
        s1.setTextColor("#1f2937");
        s1.setSubtitleColor("#4b5563");
        s1.setImageUrl("/images/1985-couple.jpg");
        s1.setImageShape("rounded-[40%_60%_70%_30%/40%_50%_60%_50%]");
        s1.setSortOrder(0);
        s1.setActive(true);
        slideRepository.save(s1);

        // Slide 2: Luxury Cars
        Slide s2 = new Slide();
        s2.setTitle("Luxury Cars Auction");
        s2.setSubtitle("Ferrari, Lamborghini, Porsche & more - Starting at $50K");
        s2.setButtonText("View Cars");
        s2.setButtonUrl("/auctions?category=CARS");
        s2.setButtonColor("#db2777");
        s2.setBackgroundColor("#fce7f3");
        s2.setBackgroundGradient("linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%)");
        s2.setTextColor("#831843");
        s2.setSubtitleColor("#9d174d");
        s2.setImageUrl("https://images.unsplash.com/photo-1544636331-e26879cd4d9b?w=600&q=80");
        s2.setImageShape("rounded-[60%_40%_30%_70%/60%_30%_70%_40%]");
        s2.setSortOrder(1);
        s2.setActive(true);
        slideRepository.save(s2);

        // Slide 3: Sign Up
        Slide s3 = new Slide();
        s3.setTitle("Start with $50K");
        s3.setSubtitle("Create your account and get instant demo funds");
        s3.setButtonText("Sign Up Free");
        s3.setButtonUrl("/register");
        s3.setButtonColor("#9333ea");
        s3.setBackgroundColor("#e9d5ff");
        s3.setBackgroundGradient("linear-gradient(135deg, #e9d5ff 0%, #d8b4fe 100%)");
        s3.setTextColor("#581c87");
        s3.setSubtitleColor("#6b21a8");
        s3.setImageUrl("https://images.unsplash.com/photo-1579621970588-a35d0e7ab9b6?w=600&q=80");
        s3.setImageShape("rounded-[70%_30%_50%_50%/40%_60%_40%_60%]");
        s3.setSortOrder(2);
        s3.setActive(true);
        slideRepository.save(s3);

        log.info("Seeded 3 default hero slides.");
    }
}
