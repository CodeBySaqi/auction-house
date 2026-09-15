package com.auctionhouse.config;

import com.auctionhouse.model.*;
import com.auctionhouse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive demo data seeder - creates 100 users with realistic activity over a week.
 */
@Component
@Order(2)
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Autowired
    public DemoDataSeeder(UserRepository userRepository,
                          AuctionRepository auctionRepository,
                          BidRepository bidRepository,
                          NotificationRepository notificationRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only seed if we don't have comprehensive data yet (less than 50 users)
        if (userRepository.count() > 50) {
            System.out.println("✓ Comprehensive demo data already exists (" + userRepository.count() + " users), skipping seed");
            return;
        }

        // Clear existing demo data to start fresh
        if (userRepository.count() > 0) {
            System.out.println("🗑️  Clearing existing demo data...");
            notificationRepository.deleteAll();
            bidRepository.deleteAll();
            auctionRepository.deleteAll();
            userRepository.deleteAll();
            System.out.println("✓ Existing data cleared");
        }

        System.out.println("🌱 Seeding comprehensive demo data (100 users, 150 auctions, ~1000 bids)...");

        // Create 100 users
        List<User> users = createUsers(100);
        System.out.println("✓ Created " + users.size() + " users");

        // Create 150 auctions
        List<Auction> auctions = createAuctions(150, users);
        System.out.println("✓ Created " + auctions.size() + " auctions");

        // Create bids over the past week
        int bidCount = createBids(auctions, users);
        System.out.println("✓ Created " + bidCount + " bids");

        System.out.println("🎉 Demo data seeding complete!");
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        String[] firstNames = {"James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda", 
                               "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
                               "Thomas", "Sarah", "Charles", "Karen", "Emma", "Olivia", "Ava", "Isabella", "Sophia",
                               "Mia", "Charlotte", "Amelia", "Harper", "Evelyn", "Liam", "Noah", "Oliver", "Elijah",
                               "Lucas", "Mason", "Logan", "Alexander", "Ethan", "Jacob", "Daniel", "Henry", "Jackson",
                               "Sebastian", "Aiden", "Matthew", "Samuel", "David", "Joseph", "Carter", "Owen"};
        
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
                              "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
                              "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
                              "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson"};

        for (int i = 0; i < count; i++) {
            User user = new User();
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String username = firstName.toLowerCase() + lastName.toLowerCase() + (random.nextInt(99) + 1);
            
            user.setUsername(username);
            user.setEmail(username + "@email.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setWalletBalance(50000 + random.nextInt(150000)); // $50K - $200K
            user.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            
            users.add(userRepository.save(user));
        }
        return users;
    }

    private List<Auction> createAuctions(int count, List<User> users) {
        List<Auction> auctions = new ArrayList<>();
        
        // Car auction data
        String[] carMakes = {"Ferrari", "Lamborghini", "Porsche", "Mercedes-Benz", "BMW", "Audi", "Tesla", "Rolls-Royce", "Bentley", "Aston Martin"};
        String[] carModels = {"Model S", "911 Turbo", "F8 Tributo", "Huracán", "AMG GT", "M8", "RS7", "Phantom", "Continental GT", "DB11"};
        String[] colors = {"Black", "White", "Red", "Blue", "Silver", "Gray", "Yellow", "Green"};
        String[] conditions = {"Excellent", "Good", "Like New", "Mint", "Very Good"};

        // Watch auction data
        String[] watchBrands = {"Rolex", "Patek Philippe", "Audemars Piguet", "Omega", "TAG Heuer", "Breitling", "Cartier", "IWC"};
        String[] watchModels = {"Submariner", "Daytona", "Nautilus", "Royal Oak", "Speedmaster", "Carrera", "Santos", "Portugieser"};
        String[] movements = {"Automatic", "Manual", "Quartz"};
        String[] caseMaterials = {"Stainless Steel", "Gold", "Platinum", "Titanium", "Rose Gold"};

        // Art auction data
        String[] artTitles = {"Sunset Over Mountains", "Abstract Composition", "Portrait of a Lady", "City at Night", 
                             "Ocean Waves", "Forest Path", "Modern Sculpture", "Vintage Photography"};
        String[] artists = {"John Smith", "Maria Garcia", "David Chen", "Sarah Johnson", "Michael Brown", "Emma Wilson"};
        String[] mediums = {"Oil on Canvas", "Watercolor", "Acrylic", "Mixed Media", "Photography", "Sculpture"};
        String[] dimensions = {"24x36 inches", "18x24 inches", "36x48 inches", "12x16 inches", "48x60 inches"};

        // Jewelry auction data
        String[] jewelryTypes = {"Diamond Ring", "Pearl Necklace", "Gold Bracelet", "Sapphire Earrings", "Ruby Pendant", "Emerald Brooch"};
        String[] metals = {"18K Gold", "Platinum", "White Gold", "Rose Gold", "Silver"};
        String[] gemstones = {"Diamond", "Sapphire", "Ruby", "Emerald", "Pearl"};
        String[] designers = {"Tiffany & Co", "Cartier", "Harry Winston", "Van Cleef", "Bulgari"};

        // Collectible auction data
        String[] collectibleTypes = {"Vintage Wine", "Rare Coins", "Antique Furniture", "Sports Memorabilia", "First Edition Books", "Comic Books"};
        String[] eras = {"Victorian", "Art Deco", "Mid-Century", "Modern", "Ancient"};
        String[] rarities = {"Common", "Uncommon", "Rare", "Very Rare", "Extremely Rare"};

        for (int i = 0; i < count; i++) {
            AuctionCategory category = AuctionCategory.values()[random.nextInt(AuctionCategory.values().length)];
            Auction auction = null;
            
            switch (category) {
                case CARS:
                    CarAuction car = new CarAuction();
                    car.setMake(carMakes[random.nextInt(carMakes.length)]);
                    car.setModel(carModels[random.nextInt(carModels.length)]);
                    car.setYear(2015 + random.nextInt(10));
                    car.setMileage(random.nextInt(100000));
                    car.setCondition(conditions[random.nextInt(conditions.length)]);
                    car.setColor(colors[random.nextInt(colors.length)]);
                    car.setTitle(car.getYear() + " " + car.getMake() + " " + car.getModel());
                    car.setStartingPrice(30000 + random.nextInt(170000)); // $30K - $200K
                    auction = car;
                    break;
                    
                case WATCHES:
                    WatchAuction watch = new WatchAuction();
                    watch.setBrand(watchBrands[random.nextInt(watchBrands.length)]);
                    watch.setModelName(watchModels[random.nextInt(watchModels.length)]);
                    watch.setMovement(movements[random.nextInt(movements.length)]);
                    watch.setCaseMaterial(caseMaterials[random.nextInt(caseMaterials.length)]);
                    watch.setReferenceNumber("REF-" + (10000 + random.nextInt(90000)));
                    watch.setTitle(watch.getBrand() + " " + watch.getModelName());
                    watch.setStartingPrice(5000 + random.nextInt(95000)); // $5K - $100K
                    auction = watch;
                    break;
                    
                case ART:
                    ArtAuction art = new ArtAuction();
                    art.setTitle(artTitles[random.nextInt(artTitles.length)]);
                    art.setArtist(artists[random.nextInt(artists.length)]);
                    art.setMedium(mediums[random.nextInt(mediums.length)]);
                    art.setYearCreated(1950 + random.nextInt(75));
                    art.setDimensions(dimensions[random.nextInt(dimensions.length)]);
                    art.setAuthenticated(random.nextBoolean());
                    art.setStartingPrice(2000 + random.nextInt(48000)); // $2K - $50K
                    auction = art;
                    break;
                    
                case JEWELRY:
                    JewelryAuction jewelry = new JewelryAuction();
                    jewelry.setTitle(jewelryTypes[random.nextInt(jewelryTypes.length)]);
                    jewelry.setMetalType(metals[random.nextInt(metals.length)]);
                    jewelry.setGemstone(gemstones[random.nextInt(gemstones.length)]);
                    jewelry.setCarat(0.5 + random.nextDouble() * 4.5);
                    jewelry.setDesigner(designers[random.nextInt(designers.length)]);
                    jewelry.setCertified(random.nextBoolean());
                    jewelry.setStartingPrice(1000 + random.nextInt(29000)); // $1K - $30K
                    auction = jewelry;
                    break;
                    
                case COLLECTIBLES:
                    CollectibleAuction collectible = new CollectibleAuction();
                    collectible.setTitle(collectibleTypes[random.nextInt(collectibleTypes.length)]);
                    collectible.setSubcategory(collectibleTypes[random.nextInt(collectibleTypes.length)]);
                    collectible.setEra(eras[random.nextInt(eras.length)]);
                    collectible.setCondition(conditions[random.nextInt(conditions.length)]);
                    collectible.setRarity(rarities[random.nextInt(rarities.length)]);
                    collectible.setProvenance("Private collection");
                    collectible.setStartingPrice(500 + random.nextInt(19500)); // $500 - $20K
                    auction = collectible;
                    break;
            }

            // Set common properties
            auction.setDescription("High-quality item in excellent condition. Perfect for collectors and enthusiasts.");
            auction.setEndTime(LocalDateTime.now().plusDays(random.nextInt(14) - 7)); // -7 to +7 days
            auction.setCreatedBy(users.get(random.nextInt(users.size())));
            
            // 70% active, 30% closed
            if (random.nextInt(100) < 70) {
                auction.setStatus(AuctionStatus.ACTIVE);
                if (auction.getEndTime().isBefore(LocalDateTime.now())) {
                    auction.setEndTime(LocalDateTime.now().plusDays(random.nextInt(7) + 1));
                }
            } else {
                auction.setStatus(AuctionStatus.CLOSED);
                auction.setEndTime(LocalDateTime.now().minusDays(random.nextInt(7) + 1));
            }

            auctions.add(auctionRepository.save(auction));
        }
        return auctions;
    }

    private int createBids(List<Auction> auctions, List<User> users) {
        int totalBids = 0;
        
        for (Auction auction : auctions) {
            // Each auction gets 0-15 bids
            int numBids = random.nextInt(16);
            double currentBid = auction.getStartingPrice();
            User highestBidder = null;
            
            for (int i = 0; i < numBids; i++) {
                User bidder = users.get(random.nextInt(users.size()));
                
                // Increase bid by 5-20%
                double bidAmount = currentBid * (1.05 + random.nextDouble() * 0.15);
                bidAmount = Math.round(bidAmount * 100.0) / 100.0;
                
                // Check if user has enough balance
                if (bidder.getWalletBalance() >= bidAmount) {
                    Bid bid = new Bid();
                    bid.setAuction(auction);
                    bid.setBidder(bidder);
                    bid.setAmount(bidAmount);
                    bid.setTimestamp(auction.getEndTime().minusHours(random.nextInt(168))); // Within last week
                    
                    bidRepository.save(bid);
                    totalBids++;
                    
                    currentBid = bidAmount;
                    highestBidder = bidder;
                    
                    // Deduct from wallet
                    bidder.setWalletBalance(bidder.getWalletBalance() - bidAmount);
                    userRepository.save(bidder);
                    
                    // Create notification for outbid users
                    if (auction.getHighestBidder() != null && !auction.getHighestBidder().equals(bidder)) {
                        Notification notif = new Notification();
                        notif.setUser(auction.getHighestBidder());
                        notif.setMessage("You were outbid on " + auction.getTitle() + ". New highest bid: $" + String.format("%,.2f", bidAmount));
                        notif.setTimestamp(bid.getTimestamp());
                        notificationRepository.save(notif);
                    }
                    
                    auction.setCurrentHighestBid(bidAmount);
                    auction.setHighestBidder(bidder);
                    auction.setBidCount(auction.getBidCount() + 1);
                }
            }
            
            auctionRepository.save(auction);
        }
        
        return totalBids;
    }
}
