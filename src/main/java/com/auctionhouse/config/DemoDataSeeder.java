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

        // Profile picture URLs (verified working)
        String[] profilePics = {
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1531427186611-ecfd6d936c79?w=200&h=200&fit=crop",
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop"
        };

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
            
            // 60% of users get profile pictures
            if (random.nextInt(100) < 60) {
                user.setProfilePicPath(profilePics[random.nextInt(profilePics.length)]);
            }
            
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
        
        // Verified car images
        String[] carImages = {
            "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1544636331-e26879cd4d9b?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1503736334956-4c8f8e92946d?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1580273916550-e323be2ae537?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1542362567-b07e54358753?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1583121274602-3e2820c69888?w=800&h=600&fit=crop"
        };

        // Watch auction data
        String[] watchBrands = {"Rolex", "Patek Philippe", "Audemars Piguet", "Omega", "TAG Heuer", "Breitling", "Cartier", "IWC"};
        String[] watchModels = {"Submariner", "Daytona", "Nautilus", "Royal Oak", "Speedmaster", "Carrera", "Santos", "Portugieser"};
        String[] movements = {"Automatic", "Manual", "Quartz"};
        String[] caseMaterials = {"Stainless Steel", "Gold", "Platinum", "Titanium", "Rose Gold"};
        
        // Verified watch images
        String[] watchImages = {
            "https://images.unsplash.com/photo-1523170335258-f5ed11844a49?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1547996160-81dfa63595aa?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1548171245-d56043048805?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1619134778706-7015533a6150?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800&h=600&fit=crop"
        };

        // Art auction data
        String[] artTitles = {"Sunset Over Mountains", "Abstract Composition", "Portrait of a Lady", "City at Night", 
                             "Ocean Waves", "Forest Path", "Modern Sculpture", "Vintage Photography"};
        String[] artists = {"John Smith", "Maria Garcia", "David Chen", "Sarah Johnson", "Michael Brown", "Emma Wilson"};
        String[] mediums = {"Oil on Canvas", "Watercolor", "Acrylic", "Mixed Media", "Photography", "Sculpture"};
        String[] dimensions = {"24x36 inches", "18x24 inches", "36x48 inches", "12x16 inches", "48x60 inches"};
        
        // Verified art images
        String[] artImages = {
            "https://images.unsplash.com/photo-1541961017774-22349e4a1262?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1549490349-8643362247b5?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=800&h=600&fit=crop"
        };

        // Jewelry auction data
        String[] jewelryTypes = {"Diamond Ring", "Pearl Necklace", "Gold Bracelet", "Sapphire Earrings", "Ruby Pendant", "Emerald Brooch"};
        String[] metals = {"18K Gold", "Platinum", "White Gold", "Rose Gold", "Silver"};
        String[] gemstones = {"Diamond", "Sapphire", "Ruby", "Emerald", "Pearl"};
        String[] designers = {"Tiffany & Co", "Cartier", "Harry Winston", "Van Cleef", "Bulgari"};
        
        // Verified jewelry images
        String[] jewelryImages = {
            "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1599643477877-530eb83abc8e?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1602751584552-8ba73aad10e1?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1573408301185-9146fe634ad0?w=800&h=600&fit=crop"
        };

        // Collectible auction data
        String[] collectibleTypes = {"Vintage Wine", "Rare Coins", "Antique Furniture", "Sports Memorabilia", "First Edition Books", "Comic Books"};
        String[] eras = {"Victorian", "Art Deco", "Mid-Century", "Modern", "Ancient"};
        String[] rarities = {"Common", "Uncommon", "Rare", "Very Rare", "Extremely Rare"};
        
        // Verified collectible images
        String[] collectibleImages = {
            "https://images.unsplash.com/photo-1558618047-3c8c76ca7d13?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1519861531473-9200262188bf?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1524578271613-d550eacf6090?w=800&h=600&fit=crop",
            "https://images.unsplash.com/photo-1618666012174-83b441c0bc76?w=800&h=600&fit=crop"
        };

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
                    car.setImageUrl(carImages[random.nextInt(carImages.length)]);
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
                    watch.setImageUrl(watchImages[random.nextInt(watchImages.length)]);
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
                    art.setImageUrl(artImages[random.nextInt(artImages.length)]);
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
                    jewelry.setImageUrl(jewelryImages[random.nextInt(jewelryImages.length)]);
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
                    collectible.setImageUrl(collectibleImages[random.nextInt(collectibleImages.length)]);
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
