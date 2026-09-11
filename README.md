# 🔨 Auction House Simulator

A **fully functional** auction house web application built with **Java OOP** and **Spring Boot**. Users can sign up, upload profile pictures, browse luxury auctions, place competitive bids, and win items when auctions close.

![Java](https://img.shields.io/badge/Java-11+-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-green?logo=spring-boot)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3-blue?logo=tailwind-css)
![License](https://img.shields.io/badge/License-MIT-yellow)

## 🎯 Features

### ✅ What's Included
- **User Authentication** — Registration, login, logout with Spring Security + BCrypt password hashing
- **Profile Management** — Upload profile pictures (JPEG, PNG, GIF, WebP), edit profile info
- **Auction Browsing** — Browse, search, and filter auctions by category
- **Bidding Engine** — Place bids with full validation (minimum bid, auction status, starting price)
- **Real-time Countdown** — Live countdown timers on each auction
- **Auto-close System** — Scheduled task closes expired auctions and declares winners automatically
- **Notification System** — Outbid alerts, win notifications, bid confirmations
- **Dashboard** — User stats, bid history, won items, active bids
- **20 Dummy Auctions** — Cars, watches, art, jewelry, and collectibles pre-seeded

### 🏗️ OOP Design Principles Demonstrated
| Principle | Implementation |
|-----------|---------------|
| **Encapsulation** | Private fields with public getters/setters, service-layer business logic |
| **Inheritance** | `CarAuction`, `ArtAuction`, `WatchAuction`, `JewelryAuction`, `CollectibleAuction` extend abstract `Auction` |
| **Polymorphism** | `AuctionService` works with `List<Auction>`, treating all types uniformly; each subclass overrides `getCategoryDetails()` and `getTypeName()` |
| **Abstraction** | Service interfaces define WHAT operations exist without exposing HOW |

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 11+** | Core language (OOP) |
| **Spring Boot 2.7** | Web framework |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | ORM / database access |
| **H2 Database** | Embedded database (zero config) |
| **Thymeleaf** | Server-side HTML templates |
| **Tailwind CSS** | Modern UI styling |
| **Google Material Icons** | Icon system |
| **Maven** | Build & dependency management |

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Run the Application
```bash
# Clone the repository
git clone https://github.com/CodeBySaqi/auction-house-simulator.git
cd auction-house-simulator

# Run with Maven
mvn spring-boot:run

# Open in browser
http://localhost:8080
```

The app automatically seeds **20 dummy auctions** and **3 demo users** on first run.

## 👤 Demo Accounts

| Username | Password | Email |
|----------|----------|-------|
| `johndoe` | `password123` | john@example.com |
| `janesmith` | `password123` | jane@example.com |
| `collector99` | `password123` | collector@example.com |

> 💡 Every new account starts with **$100,000** in demo wallet balance!

## 📂 Project Structure

```
src/main/java/com/auctionhouse/
├── AuctionHouseApplication.java          # Main entry point
├── model/                                # Entity classes (OOP hierarchy)
│   ├── User.java
│   ├── Auction.java                      # Abstract base class
│   ├── CarAuction.java                   # Extends Auction
│   ├── ArtAuction.java                   # Extends Auction
│   ├── WatchAuction.java                 # Extends Auction
│   ├── JewelryAuction.java               # Extends Auction
│   ├── CollectibleAuction.java           # Extends Auction
│   ├── Bid.java
│   ├── Notification.java
│   ├── AuctionStatus.java
│   └── AuctionCategory.java
├── repository/                           # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── AuctionRepository.java
│   ├── BidRepository.java
│   └── NotificationRepository.java
├── service/                              # Business logic layer
│   ├── UserService.java                  # Implements UserDetailsService
│   ├── AuctionService.java               # + @Scheduled auto-close
│   ├── BidService.java                   # Core bidding engine
│   ├── NotificationService.java
│   └── FileStorageService.java
├── controller/                           # Web controllers
│   ├── HomeController.java
│   ├── AuthController.java
│   ├── AuctionController.java
│   ├── BidController.java
│   └── ProfileController.java
├── config/                               # Configuration
│   ├── SecurityConfig.java
│   ├── PasswordEncoderConfig.java
│   ├── WebConfig.java
│   ├── GlobalModelAdvice.java            # @ControllerAdvice
│   └── DataSeeder.java                   # Seeds dummy data
├── dto/                                  # Data Transfer Objects
│   ├── UserRegistrationDTO.java
│   └── BidDTO.java
└── exception/                            # Custom exceptions
    ├── AuctionClosedException.java
    ├── BidTooLowException.java
    └── GlobalExceptionHandler.java
```

## 🔑 Key Code Examples

### Polymorphism in Action
```java
// AuctionService works with ALL auction types uniformly
List<Auction> auctions = auctionService.getActiveAuctions();
for (Auction auction : auctions) {
    System.out.println(auction.getCategoryDetails()); // Each type returns its own details
    System.out.println(auction.getTypeName());        // "Car", "Art", "Watch", etc.
}
```

### Bidding Engine (Encapsulation)
```java
public Bid placeBid(Auction auction, User bidder, double amount) {
    if (auction.getStatus() != AuctionStatus.ACTIVE)
        throw new AuctionClosedException("Auction is closed.");
    if (amount < auction.getMinimumBid())
        throw new BidTooLowException("Bid too low!");
    
    User previous = auction.getHighestBidder();
    Bid bid = new Bid(bidder, auction, amount);
    auction.acceptBid(bid);  // Internal state management
    return bidRepository.save(bid);
}
```

### Inheritance Hierarchy
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "auction_type")
public abstract class Auction {
    // Common fields: title, price, bids, endTime, status...
    public abstract String getCategoryDetails();
    public abstract String getTypeName();
}

@Entity @DiscriminatorValue("CAR")
public class CarAuction extends Auction {
    private String make, model;
    private int year, mileage;
}
```

## 🏷️ Sample Auctions

| Category | Items |
|----------|-------|
| 🚗 Cars | 1967 Mustang Shelby ($150K), Lamborghini Huracán ($280K), Mercedes 300SL ($1.2M), Porsche GT3 RS ($320K) |
| ⌚ Watches | Rolex Daytona Platinum ($75K), Patek Philippe Nautilus ($200K), AP Royal Oak ($45K) |
| 🎨 Art | Banksy "Girl with Balloon" ($120K), Warhol Marilyn ($500K), Hokusai "Great Wave" ($35K) |
| 💎 Jewelry | 5.2ct Diamond Ring ($95K), Cartier Love Bracelet ($18K), Kashmir Sapphire Necklace ($350K) |
| 🏺 Collectibles | First Edition Harry Potter ($55K), MJ Rookie Card ($25K), Apollo 11 Patch ($80K) |

## 🗄️ Database

Uses **H2 embedded database** (zero configuration).

- **H2 Console:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/auctiondb`
  - Username: `sa`
  - Password: *(empty)*

## 📝 License

This is a demo/educational project. All auction items and data are fictional.
