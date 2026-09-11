package com.auctionhouse.config;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.AuctionRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * DataSeeder - seeds the database with dummy auction data on startup.
 * Only seeds if database is empty (first run).
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
        if (auctionRepository.count() > 0) {
            return; // Already seeded
        }

        // Create demo users
        User demoUser1 = new User("johndoe", "john@example.com", passwordEncoder.encode("password123"));
        User demoUser2 = new User("janesmith", "jane@example.com", passwordEncoder.encode("password123"));
        User demoUser3 = new User("collector99", "collector@example.com", passwordEncoder.encode("password123"));
        userRepository.save(demoUser1);
        userRepository.save(demoUser2);
        userRepository.save(demoUser3);

        // === CARS ===
        CarAuction car1 = new CarAuction();
        car1.setTitle("1967 Ford Mustang Shelby GT500");
        car1.setDescription("An iconic American muscle car in pristine condition. This '67 Shelby GT500 features the original 428 cubic inch V8 engine, 4-speed manual transmission, and Nightmist Blue paint with white Lemans stripes. Fully documented with original build sheet. One of only 2,048 produced.");
        car1.setImageUrl("https://images.unsplash.com/photo-1584345604476-8ec5e12e42dd?w=800");
        car1.setStartingPrice(150000.0);
        car1.setEndTime(LocalDateTime.now().plusDays(3));
        car1.setMake("Ford");
        car1.setModel("Mustang Shelby GT500");
        car1.setYear(1967);
        car1.setMileage(42000);
        car1.setCondition("Excellent");
        car1.setColor("Nightmist Blue");
        auctionRepository.save(car1);

        CarAuction car2 = new CarAuction();
        car2.setTitle("2024 Lamborghini Huracán Tecnica");
        car2.setDescription("Brand new 2024 Huracán Tecnica with only 200 delivery miles. Naturally aspirated 5.2L V10 producing 631 HP. Rear-wheel drive, rear-wheel steering, and LDVI system. Verde Mantis exterior with Nero Ade interior.");
        car2.setImageUrl("https://images.unsplash.com/photo-1544636331-e26879cd4d9b?w=800");
        car2.setStartingPrice(280000.0);
        car2.setEndTime(LocalDateTime.now().plusDays(5));
        car2.setMake("Lamborghini");
        car2.setModel("Huracán Tecnica");
        car2.setYear(2024);
        car2.setMileage(200);
        car2.setCondition("New");
        car2.setColor("Verde Mantis");
        auctionRepository.save(car2);

        CarAuction car3 = new CarAuction();
        car3.setTitle("1955 Mercedes-Benz 300SL Gullwing");
        car3.setDescription("The legendary Gullwing. Silver metallic over red leather interior. Matching numbers, complete with factory luggage set and original tool kit. Recent mechanical restoration by Mercedes-Benz Classic Center. A true collector's dream.");
        car3.setImageUrl("https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800");
        car3.setStartingPrice(1200000.0);
        car3.setEndTime(LocalDateTime.now().plusDays(7));
        car3.setMake("Mercedes-Benz");
        car3.setModel("300SL Gullwing");
        car3.setYear(1955);
        car3.setMileage(68000);
        car3.setCondition("Restored");
        car3.setColor("Silver Metallic");
        auctionRepository.save(car3);

        CarAuction car4 = new CarAuction();
        car4.setTitle("2023 Porsche 911 GT3 RS");
        car4.setDescription("The ultimate track-focused 911. 4.0L flat-six producing 518 HP, 7-speed PDK. Weissach Package with carbon fiber anti-roll bars and cage. Python Green with black wheels and carbon fiber accents. 1,800 careful miles.");
        car4.setImageUrl("https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=800");
        car4.setStartingPrice(320000.0);
        car4.setEndTime(LocalDateTime.now().plusDays(4));
        car4.setMake("Porsche");
        car4.setModel("911 GT3 RS");
        car4.setYear(2023);
        car4.setMileage(1800);
        car4.setCondition("Like New");
        car4.setColor("Python Green");
        auctionRepository.save(car4);

        // === WATCHES ===
        WatchAuction watch1 = new WatchAuction();
        watch1.setTitle("Rolex Cosmograph Daytona Platinum");
        watch1.setDescription("The pinnacle of the Daytona line. 40mm platinum case with ice blue dial and chestnut brown Cerachrom bezel. Reference 126506. Full box and papers, 2023 model. The ultimate chronograph wristwatch.");
        watch1.setImageUrl("https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800");
        watch1.setStartingPrice(75000.0);
        watch1.setEndTime(LocalDateTime.now().plusDays(2));
        watch1.setBrand("Rolex");
        watch1.setModelName("Cosmograph Daytona");
        watch1.setMovement("Automatic Cal. 4131");
        watch1.setCaseMaterial("950 Platinum");
        watch1.setReferenceNumber("126506");
        auctionRepository.save(watch1);

        WatchAuction watch2 = new WatchAuction();
        watch2.setTitle("Patek Philippe Nautilus 5711/1A");
        watch2.setDescription("The most sought-after sports watch in the world. Stainless steel 40mm case with the iconic blue-black gradient dial. Reference 5711/1A-010. Complete set with box, papers, and service history. Discontinued model.");
        watch2.setImageUrl("https://images.unsplash.com/photo-1523170335258-f5ed11844a49?w=800");
        watch2.setStartingPrice(200000.0);
        watch2.setEndTime(LocalDateTime.now().plusDays(6));
        watch2.setBrand("Patek Philippe");
        watch2.setModelName("Nautilus");
        watch2.setMovement("Automatic Cal. 26-330 S C");
        watch2.setCaseMaterial("Stainless Steel");
        watch2.setReferenceNumber("5711/1A-010");
        auctionRepository.save(watch2);

        WatchAuction watch3 = new WatchAuction();
        watch3.setTitle("Audemars Piguet Royal Oak 15500ST");
        watch3.setDescription("The iconic luxury sports watch designed by Gerald Genta. 41mm stainless steel case with the signature blue 'Grande Tapisserie' dial. Reference 15500ST.OO.1220ST.01. Excellent condition with full set.");
        watch3.setImageUrl("https://images.unsplash.com/photo-1548171915-e79a380a2a4b?w=800");
        watch3.setStartingPrice(45000.0);
        watch3.setEndTime(LocalDateTime.now().plusDays(3));
        watch3.setBrand("Audemars Piguet");
        watch3.setModelName("Royal Oak");
        watch3.setMovement("Automatic Cal. 4302");
        watch3.setCaseMaterial("Stainless Steel");
        watch3.setReferenceNumber("15500ST.OO.1220ST.01");
        auctionRepository.save(watch3);

        // === ART ===
        ArtAuction art1 = new ArtAuction();
        art1.setTitle("Banksy — Girl with Balloon (Signed Print)");
        art1.setDescription("An authenticated signed screenprint by the legendary street artist Banksy. 'Girl with Balloon' (2004) is one of the most recognized works in contemporary art. Edition of 150. Framed and in excellent condition. Certificate of authenticity from Pest Control included.");
        art1.setImageUrl("https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?w=800");
        art1.setStartingPrice(120000.0);
        art1.setEndTime(LocalDateTime.now().plusDays(5));
        art1.setArtist("Banksy");
        art1.setMedium("Screenprint on paper");
        art1.setYearCreated(2004);
        art1.setDimensions("28 x 20 inches");
        art1.setAuthenticated(true);
        auctionRepository.save(art1);

        ArtAuction art2 = new ArtAuction();
        art2.setTitle("Andy Warhol — Marilyn Monroe Silkscreen");
        art2.setDescription("Original Warhol silkscreen from the Marilyn series. Vivid colors on a 36x36 inch canvas. Authenticated by the Andy Warhol Art Authentication Board. Provenance from a major New York gallery. A defining piece of Pop Art.");
        art2.setImageUrl("https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800");
        art2.setStartingPrice(500000.0);
        art2.setEndTime(LocalDateTime.now().plusDays(8));
        art2.setArtist("Andy Warhol");
        art2.setMedium("Silkscreen on canvas");
        art2.setYearCreated(1967);
        art2.setDimensions("36 x 36 inches");
        art2.setAuthenticated(true);
        auctionRepository.save(art2);

        ArtAuction art3 = new ArtAuction();
        art3.setTitle("Japanese Woodblock Print — The Great Wave (c. 1831)");
        art3.setDescription("An original ukiyo-e woodblock print by Katsushika Hokusai from the series 'Thirty-six Views of Mount Fuji.' This impression is from the early printing with strong color saturation. A masterpiece of Japanese art.");
        art3.setImageUrl("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800");
        art3.setStartingPrice(35000.0);
        art3.setEndTime(LocalDateTime.now().plusDays(4));
        art3.setArtist("Katsushika Hokusai");
        art3.setMedium("Woodblock print (nishiki-e)");
        art3.setYearCreated(1831);
        art3.setDimensions("10 x 15 inches");
        art3.setAuthenticated(true);
        auctionRepository.save(art3);

        // === JEWELRY ===
        JewelryAuction jewel1 = new JewelryAuction();
        jewel1.setTitle("5.2 Carat Diamond Solitaire Ring");
        jewel1.setDescription("A breathtaking 5.2 carat round brilliant cut diamond set in a platinum band. GIA certified D color, VVS1 clarity, Excellent cut. The ultimate expression of luxury and elegance. Comes with GIA certificate and appraisal.");
        jewel1.setImageUrl("https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=800");
        jewel1.setStartingPrice(95000.0);
        jewel1.setEndTime(LocalDateTime.now().plusDays(6));
        jewel1.setMetalType("Platinum");
        jewel1.setGemstone("Diamond (Round Brilliant)");
        jewel1.setCarat(5.2);
        jewel1.setDesigner("Custom");
        jewel1.setCertified(true);
        auctionRepository.save(jewel1);

        JewelryAuction jewel2 = new JewelryAuction();
        jewel2.setTitle("Vintage Cartier Love Bracelet — Yellow Gold");
        jewel2.setDescription("An iconic Cartier Love bracelet in 18K yellow gold. Size 17. Includes original screwdriver and box. Vintage piece from the 1980s in excellent condition with beautiful patina. Serial number verified with Cartier.");
        jewel2.setImageUrl("https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=800");
        jewel2.setStartingPrice(18000.0);
        jewel2.setEndTime(LocalDateTime.now().plusDays(2));
        jewel2.setMetalType("18K Yellow Gold");
        jewel2.setGemstone("None");
        jewel2.setCarat(0);
        jewel2.setDesigner("Cartier");
        jewel2.setCertified(true);
        auctionRepository.save(jewel2);

        JewelryAuction jewel3 = new JewelryAuction();
        jewel3.setTitle("Kashmir Sapphire & Diamond Necklace");
        jewel3.setDescription("A museum-quality necklace featuring a 12-carat unheated Kashmir sapphire surrounded by 8 carats of brilliant-cut diamonds. Set in platinum. Origin confirmed by Gübelin Gem Lab. A once-in-a-lifetime piece.");
        jewel3.setImageUrl("https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=800");
        jewel3.setStartingPrice(350000.0);
        jewel3.setEndTime(LocalDateTime.now().plusDays(7));
        jewel3.setMetalType("Platinum");
        jewel3.setGemstone("Kashmir Sapphire");
        jewel3.setCarat(12.0);
        jewel3.setDesigner("Van Cleef & Arpels");
        jewel3.setCertified(true);
        auctionRepository.save(jewel3);

        // === COLLECTIBLES ===
        CollectibleAuction col1 = new CollectibleAuction();
        col1.setTitle("First Edition Harry Potter and the Philosopher's Stone");
        col1.setDescription("A true first edition, first printing of J.K. Rowling's Harry Potter and the Philosopher's Stone (1997, Bloomsbury). One of only 500 copies in the first print run. Hardcover in very good condition with original dust jacket.");
        col1.setImageUrl("https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800");
        col1.setStartingPrice(55000.0);
        col1.setEndTime(LocalDateTime.now().plusDays(5));
        col1.setSubcategory("Rare Books");
        col1.setEra("1990s");
        col1.setCondition("Very Good");
        col1.setRarity("Ultra Rare");
        col1.setProvenance("Private collection, UK");
        auctionRepository.save(col1);

        CollectibleAuction col2 = new CollectibleAuction();
        col2.setTitle("Signed Michael Jordan 1986 Fleer Rookie Card (PSA 10)");
        col2.setDescription("The holy grail of basketball cards. 1986 Fleer Michael Jordan #57 rookie card, graded PSA Gem Mint 10 and personally signed by Jordan. One of the finest known examples. Beckett authenticated signature.");
        col2.setImageUrl("https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800");
        col2.setStartingPrice(25000.0);
        col2.setEndTime(LocalDateTime.now().plusDays(3));
        col2.setSubcategory("Sports Memorabilia");
        col2.setEra("1980s");
        col2.setCondition("PSA Gem Mint 10");
        col2.setRarity("Extremely Rare");
        col2.setProvenance("Private sports collection");
        auctionRepository.save(col2);

        CollectibleAuction col3 = new CollectibleAuction();
        col3.setTitle("Apollo 11 Moon Landing — Signed Mission Patch");
        col3.setDescription("Original Apollo 11 mission patch (Beta cloth, 4 inches) signed by all three crew members: Neil Armstrong, Buzz Aldrin, and Michael Collins. Accompanied by a letter of provenance from the personal collection of a NASA engineer.");
        col3.setImageUrl("https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800");
        col3.setStartingPrice(80000.0);
        col3.setEndTime(LocalDateTime.now().plusDays(6));
        col3.setSubcategory("Space Memorabilia");
        col3.setEra("1969");
        col3.setCondition("Excellent");
        col3.setRarity("Ultra Rare");
        col3.setProvenance("NASA engineer's estate");
        auctionRepository.save(col3);

        CollectibleAuction col4 = new CollectibleAuction();
        col4.setTitle("Vintage 1952 Topps Mickey Mantle #311 (PSA 7)");
        col4.setDescription("The most iconic baseball card ever produced. 1952 Topps Mickey Mantle #311, graded PSA 7 NM. Exceptional centering and sharp corners. One of the finest examples available. A cornerstone of any serious collection.");
        col4.setImageUrl("https://images.unsplash.com/photo-1529768167801-9173d94c2a42?w=800");
        col4.setStartingPrice(450000.0);
        col4.setEndTime(LocalDateTime.now().plusDays(9));
        col4.setSubcategory("Sports Cards");
        col4.setEra("1950s");
        col4.setCondition("PSA 7 NM");
        col4.setRarity("Legendary");
        col4.setProvenance("Estate of a Hall of Fame collector");
        auctionRepository.save(col4);

        System.out.println("✓ Seeded 20 dummy auctions and 3 demo users successfully!");
    }
}
