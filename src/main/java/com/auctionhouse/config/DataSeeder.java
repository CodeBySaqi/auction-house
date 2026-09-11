package com.auctionhouse.config;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.AuctionRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * DataSeeder - seeds the database with dummy auction data on startup.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataSeeder(AuctionRepository auctionRepository, UserRepository userRepository,
                     PasswordEncoder passwordEncoder) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (auctionRepository.count() > 0) return;

        // Create demo users
        User u1 = new User("johndoe", "john@example.com", passwordEncoder.encode("password123"));
        User u2 = new User("janesmith", "jane@example.com", passwordEncoder.encode("password123"));
        User u3 = new User("collector99", "collector@example.com", passwordEncoder.encode("password123"));
        userRepository.save(u1);
        userRepository.save(u2);
        userRepository.save(u3);

        Random rand = new Random();

        // Helper to create varied end times (15 min to 1 hour)
        // Some ending very soon for excitement
        LocalDateTime[] endTimes = {
            LocalDateTime.now().plusMinutes(15),
            LocalDateTime.now().plusMinutes(25),
            LocalDateTime.now().plusMinutes(35),
            LocalDateTime.now().plusMinutes(45),
            LocalDateTime.now().plusMinutes(50),
            LocalDateTime.now().plusMinutes(55),
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusHours(1).plusMinutes(10),
            LocalDateTime.now().plusHours(1).plusMinutes(20),
            LocalDateTime.now().plusHours(1).plusMinutes(30),
        };

        int idx = 0;

        // === CARS (8 auctions) ===
        saveCar("1967 Ford Mustang Shelby GT500", "An iconic American muscle car in pristine condition. Original 428 V8, 4-speed manual, Nightmist Blue with white Lemans stripes. Fully documented.", "https://images.unsplash.com/photo-1584345604476-8ec5e12e42dd?w=800", 150000, endTimes[idx++ % 10], "Ford", "Mustang Shelby GT500", 1967, 42000, "Excellent", "Nightmist Blue");
        saveCar("2024 Lamborghini Huracán Tecnica", "Brand new with 200 delivery miles. 5.2L V10, 631 HP, rear-wheel drive. Verde Mantis exterior with Nero Ade interior.", "https://images.unsplash.com/photo-1544636331-e26879cd4d9b?w=800", 280000, endTimes[idx++ % 10], "Lamborghini", "Huracán Tecnica", 2024, 200, "New", "Verde Mantis");
        saveCar("1955 Mercedes-Benz 300SL Gullwing", "The legendary Gullwing. Silver metallic, red leather. Matching numbers, factory luggage, original tools. Restored by Mercedes Classic.", "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800", 1200000, endTimes[idx++ % 10], "Mercedes-Benz", "300SL Gullwing", 1955, 68000, "Restored", "Silver Metallic");
        saveCar("2023 Porsche 911 GT3 RS", "Track-focused 911. 4.0L flat-six, 518 HP, PDK. Weissach Package, Python Green, carbon fiber. 1,800 miles.", "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=800", 320000, endTimes[idx++ % 10], "Porsche", "911 GT3 RS", 2023, 1800, "Like New", "Python Green");
        saveCar("1961 Jaguar E-Type Series 1", "The most beautiful car ever made. 3.8L inline-6, original British Racing Green. Matching numbers, comprehensive history file.", "https://images.unsplash.com/photo-1583121274602-3e2820c69888?w=800", 185000, endTimes[idx++ % 10], "Jaguar", "E-Type Series 1", 1961, 54000, "Excellent", "British Racing Green");
        saveCar("2024 Ferrari SF90 Stradale", "Hybrid hypercar. 986 HP combined, 0-60 in 2.5s. Rosso Corsa with Nero interior. Full carbon package, only 500 miles.", "https://images.unsplash.com/photo-1592198084033-aade902d1aae?w=800", 625000, endTimes[idx++ % 10], "Ferrari", "SF90 Stradale", 2024, 500, "New", "Rosso Corsa");
        saveCar("1989 Nissan Skyline GT-R R32", "Godzilla. Twin-turbo RB26DETT, ATTESA AWD. Gunmetal Grey, 62K miles. Import documentation, fully serviced.", "https://images.unsplash.com/photo-1626668011687-8a114cf5a34c?w=800", 95000, endTimes[idx++ % 10], "Nissan", "Skyline GT-R R32", 1989, 62000, "Very Good", "Gunmetal Grey");
        saveCar("2022 McLaren 765LT Spider", "Longtail convertible. 755 HP twin-turbo V8, active aero. MSO bespoke paint, carbon everything. 2,100 miles.", "https://images.unsplash.com/photo-1621135802920-133df287f89c?w=800", 440000, endTimes[idx++ % 10], "McLaren", "765LT Spider", 2022, 2100, "Excellent", "MSO Aurora Blue");

        // === WATCHES (7 auctions) ===
        saveWatch("Rolex Cosmograph Daytona Platinum", "40mm platinum, ice blue dial, chestnut Cerachrom bezel. Ref 126506. Full box & papers, 2023.", "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800", 75000, endTimes[idx++ % 10], "Rolex", "Cosmograph Daytona", "Automatic Cal. 4131", "950 Platinum", "126506");
        saveWatch("Patek Philippe Nautilus 5711/1A", "The most sought-after sports watch. Steel 40mm, blue-black gradient dial. Discontinued model, complete set.", "https://images.unsplash.com/photo-1523170335258-f5ed11844a49?w=800", 200000, endTimes[idx++ % 10], "Patek Philippe", "Nautilus", "Automatic Cal. 26-330 S C", "Stainless Steel", "5711/1A-010");
        saveWatch("Audemars Piguet Royal Oak 15500ST", "Gerald Genta icon. 41mm steel, blue Grande Tapisserie dial. Excellent condition, full set.", "https://images.unsplash.com/photo-1548171915-e79a380a2a4b?w=800", 45000, endTimes[idx++ % 10], "Audemars Piguet", "Royal Oak", "Automatic Cal. 4302", "Stainless Steel", "15500ST");
        saveWatch("Omega Speedmaster Moonwatch Professional", "The watch that went to the Moon. Hesalite crystal, manual wind. Full kit with NASA-style packaging.", "https://images.unsplash.com/photo-1614164185128-e4ec99c436d7?w=800", 6500, endTimes[idx++ % 10], "Omega", "Speedmaster Moonwatch", "Manual Cal. 3861", "Stainless Steel", "310.30.42.50.01.001");
        saveWatch("Richard Mille RM 011 Flyback Chronograph", "Ultra-luxury. Titanium case, skeletonized dial. Limited edition, box and papers. A statement piece.", "https://images.unsplash.com/photo-1522312346375-d1a52e2b99b3?w=800", 165000, endTimes[idx++ % 10], "Richard Mille", "RM 011", "Automatic Flyback", "Grade 5 Titanium", "RM 011-03");
        saveWatch("Cartier Santos de Cartier Medium", "The original pilot's watch. Steel and gold, blue dial. QuickSwitch bracelet system. Full set 2023.", "https://images.unsplash.com/photo-1548171245-1d66c4b29e1a?w=800", 8500, endTimes[idx++ % 10], "Cartier", "Santos de Cartier", "Automatic Cal. 1847 MC", "Steel & 18K Gold", "WSSA0029");
        saveWatch("Grand Seiko Snowflake SBGA211", "Spring Drive movement, power reserve indicator. Titanium case, textured 'Snowflake' dial. Japanese perfection.", "https://images.unsplash.com/photo-1509941943102-10c232fc9e89?w=800", 5800, endTimes[idx++ % 10], "Grand Seiko", "Snowflake", "Spring Drive Cal. 9R65", "High-Intensity Titanium", "SBGA211");

        // === ART (7 auctions) ===
        saveArt("Banksy — Girl with Balloon (Signed Print)", "Authenticated signed screenprint. Edition of 150. Framed, excellent condition. Pest Control certificate.", "https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?w=800", 120000, endTimes[idx++ % 10], "Banksy", "Screenprint on paper", 2004, "28 x 20 inches", true);
        saveArt("Andy Warhol — Marilyn Monroe Silkscreen", "Original from the Marilyn series. 36x36 canvas. Authenticated by Warhol Art Board. NY gallery provenance.", "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800", 500000, endTimes[idx++ % 10], "Andy Warhol", "Silkscreen on canvas", 1967, "36 x 36 inches", true);
        saveArt("Japanese Woodblock Print — The Great Wave (c. 1831)", "Hokusai's masterpiece. Early printing, strong colors. A defining work of Japanese art.", "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800", 35000, endTimes[idx++ % 10], "Katsushika Hokusai", "Woodblock print (nishiki-e)", 1831, "10 x 15 inches", true);
        saveArt("Jean-Michel Basquiat — Untitled (Skull)", "Original 1981 work on paper. Acrylic and oilstick. Authenticated by the Basquiat estate. Museum quality.", "https://images.unsplash.com/photo-1569172122301-bc5008bc09c5?w=800", 380000, endTimes[idx++ % 10], "Jean-Michel Basquiat", "Acrylic & oilstick on paper", 1981, "30 x 22 inches", true);
        saveArt("Yayoi Kusama — Infinity Net (Yellow)", "Large-scale oil on canvas. 2006 work, 72x72 inches. From the legendary Infinity Net series.", "https://images.unsplash.com/photo-1549887534-1541e9326642?w=800", 250000, endTimes[idx++ % 10], "Yayoi Kusama", "Oil on canvas", 2006, "72 x 72 inches", true);
        saveArt("KAWS — Companion (Open Edition)", "Vinyl figure, 11 inches. Still in original box. The iconic crossed-out eyes design. 2016 release.", "https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=800", 3500, endTimes[idx++ % 10], "KAWS", "Vinyl sculpture", 2016, "11 x 5 x 3 inches", false);
        saveArt("Claude Monet — Water Lilies Study", "Oil on canvas study for the Musée de l'Orangerie murals. Soft impressionist palette. Authenticated.", "https://images.unsplash.com/photo-1580136579312-94651dfd596d?w=800", 420000, endTimes[idx++ % 10], "Claude Monet", "Oil on canvas", 1906, "24 x 30 inches", true);

        // === JEWELRY (5 auctions) ===
        saveJewelry("5.2 Carat Diamond Solitaire Ring", "Round brilliant, platinum band. GIA D color, VVS1 clarity, Excellent cut. GIA certificate included.", "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=800", 95000, endTimes[idx++ % 10], "Platinum", "Diamond (Round Brilliant)", 5.2, "Custom", true);
        saveJewelry("Vintage Cartier Love Bracelet — Yellow Gold", "18K yellow gold, size 17. 1980s vintage, excellent patina. Serial verified with Cartier.", "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=800", 18000, endTimes[idx++ % 10], "18K Yellow Gold", "None", 0, "Cartier", true);
        saveJewelry("Kashmir Sapphire & Diamond Necklace", "12ct unheated Kashmir sapphire, 8ct diamonds. Platinum. Gübelin certified. Museum quality.", "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=800", 350000, endTimes[idx++ % 10], "Platinum", "Kashmir Sapphire", 12.0, "Van Cleef & Arpels", true);
        saveJewelry("Emerald & Diamond Art Deco Brooch", "1920s Art Deco, Colombian emerald (8ct), 4ct diamonds. Platinum setting. Museum exhibited.", "https://images.unsplash.com/photo-1515562141589-67f0d569b6c6?w=800", 78000, endTimes[idx++ % 10], "Platinum", "Colombian Emerald", 8.0, "Cartier (1925)", true);
        saveJewelry("Tiffany & Co. Diamond Tennis Bracelet", "18K white gold, 15ct total diamond weight. F-G color, VS clarity. Tiffany signed and stamped.", "https://images.unsplash.com/photo-1602751584552-8ba73aad10e1?w=800", 22000, endTimes[idx++ % 10], "18K White Gold", "Diamonds", 15.0, "Tiffany & Co.", true);

        // === COLLECTIBLES (8 auctions) ===
        saveCollectible("First Edition Harry Potter and the Philosopher's Stone", "True first edition, first printing (1997, Bloomsbury). One of only 500 copies. Hardcover with dust jacket.", "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800", 55000, endTimes[idx++ % 10], "Rare Books", "1990s", "Very Good", "Ultra Rare", "Private collection, UK");
        saveCollectible("Signed Michael Jordan 1986 Fleer Rookie Card (PSA 10)", "PSA Gem Mint 10, personally signed by Jordan. Beckett authenticated signature. Finest known examples.", "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800", 25000, endTimes[idx++ % 10], "Sports Memorabilia", "1980s", "PSA Gem Mint 10", "Extremely Rare", "Private sports collection");
        saveCollectible("Apollo 11 Moon Landing — Signed Mission Patch", "Original Beta cloth patch signed by Armstrong, Aldrin, and Collins. Letter of provenance from NASA engineer.", "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800", 80000, endTimes[idx++ % 10], "Space Memorabilia", "1969", "Excellent", "Ultra Rare", "NASA engineer's estate");
        saveCollectible("1952 Topps Mickey Mantle #311 (PSA 7)", "Most iconic baseball card ever. PSA 7 NM, exceptional centering. Cornerstone of any collection.", "https://images.unsplash.com/photo-1529768167801-9173d94c2a42?w=800", 450000, endTimes[idx++ % 10], "Sports Cards", "1950s", "PSA 7 NM", "Legendary", "Hall of Fame collector");
        saveCollectible("Original Apple-1 Computer (1976)", "Working Apple-1 by Steve Wozniak. One of ~60 known survivors. Original cassette interface. Museum piece.", "https://images.unsplash.com/photo-1517077304055-6e89abbf09b0?w=800", 375000, endTimes[idx++ % 10], "Technology", "1976", "Working Condition", "Legendary", "Silicon Valley collector");
        saveCollectible("1966 Batman TV Series Batmobile (1:1 Scale)", "Official George Barris reproduction. Road-legal, custom built. One of 5 authorized replicas in existence.", "https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?w=800", 165000, endTimes[idx++ % 10], "Pop Culture", "1966", "Show Quality", "Ultra Rare", "Private entertainment collection");
        saveCollectible("Stradivarius Violin (Attributed, c. 1720)", "Golden period Stradivarius attribution. Played by concertmasters. Certified by international experts.", "https://images.unsplash.com/photo-1612225330812-01a9c73fcbdc?w=800", 280000, endTimes[idx++ % 10], "Musical Instruments", "1720s", "Playable", "Legendary", "European conservatory");
        saveCollectible("Charizard 1st Edition Holo (PSA 10, 1999)", "The holy grail of Pokémon cards. Base Set 1st Edition, PSA Gem Mint 10. Investment grade.", "https://images.unsplash.com/photo-1613771404784-3a5686aa2be3?w=800", 150000, endTimes[idx++ % 10], "Trading Cards", "1999", "PSA 10 Gem Mint", "Legendary", "Private TCG collection");

        System.out.println("✓ Seeded " + auctionRepository.count() + " auctions and 3 demo users!");
    }

    // === Helper methods ===

    private void saveCar(String title, String desc, String img, double price, LocalDateTime end, String make, String model, int year, int mileage, String condition, String color) {
        CarAuction a = new CarAuction();
        a.setTitle(title); a.setDescription(desc); a.setImageUrl(img);
        a.setStartingPrice(price); a.setEndTime(end);
        a.setMake(make); a.setModel(model); a.setYear(year);
        a.setMileage(mileage); a.setCondition(condition); a.setColor(color);
        auctionRepository.save(a);
    }

    private void saveWatch(String title, String desc, String img, double price, LocalDateTime end, String brand, String modelName, String movement, String caseMat, String ref) {
        WatchAuction a = new WatchAuction();
        a.setTitle(title); a.setDescription(desc); a.setImageUrl(img);
        a.setStartingPrice(price); a.setEndTime(end);
        a.setBrand(brand); a.setModelName(modelName); a.setMovement(movement);
        a.setCaseMaterial(caseMat); a.setReferenceNumber(ref);
        auctionRepository.save(a);
    }

    private void saveArt(String title, String desc, String img, double price, LocalDateTime end, String artist, String medium, int yearCreated, String dims, boolean auth) {
        ArtAuction a = new ArtAuction();
        a.setTitle(title); a.setDescription(desc); a.setImageUrl(img);
        a.setStartingPrice(price); a.setEndTime(end);
        a.setArtist(artist); a.setMedium(medium); a.setYearCreated(yearCreated);
        a.setDimensions(dims); a.setAuthenticated(auth);
        auctionRepository.save(a);
    }

    private void saveJewelry(String title, String desc, String img, double price, LocalDateTime end, String metal, String gem, double carat, String designer, boolean cert) {
        JewelryAuction a = new JewelryAuction();
        a.setTitle(title); a.setDescription(desc); a.setImageUrl(img);
        a.setStartingPrice(price); a.setEndTime(end);
        a.setMetalType(metal); a.setGemstone(gem); a.setCarat(carat);
        a.setDesigner(designer); a.setCertified(cert);
        auctionRepository.save(a);
    }

    private void saveCollectible(String title, String desc, String img, double price, LocalDateTime end, String subcat, String era, String condition, String rarity, String provenance) {
        CollectibleAuction a = new CollectibleAuction();
        a.setTitle(title); a.setDescription(desc); a.setImageUrl(img);
        a.setStartingPrice(price); a.setEndTime(end);
        a.setSubcategory(subcat); a.setEra(era); a.setCondition(condition);
        a.setRarity(rarity); a.setProvenance(provenance);
        auctionRepository.save(a);
    }
}
