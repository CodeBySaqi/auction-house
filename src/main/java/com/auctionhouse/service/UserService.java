package com.auctionhouse.service;

import com.auctionhouse.dto.UserRegistrationDTO;
import com.auctionhouse.model.User;
import com.auctionhouse.repository.AuctionRepository;
import com.auctionhouse.repository.BidRepository;
import com.auctionhouse.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

/**
 * UserService - handles all user-related business logic.
 * Implements UserDetailsService for Spring Security integration.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       AuctionRepository auctionRepository,
                       BidRepository bidRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Block deactivated users from logging in
        if (!user.isActive()) {
            throw new UsernameNotFoundException("This account has been deactivated. Contact an administrator.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }

    /**
     * Register a new user.
     */
    @Transactional
    public User register(UserRegistrationDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setWalletBalance(100000.0);

        return userRepository.save(user);
    }

    /**
     * Find user by username.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by ID.
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Update user profile.
     */
    @Transactional
    public User updateProfile(User user) {
        return userRepository.save(user);
    }

    /**
     * Update profile picture path.
     */
    @Transactional
    public void updateProfilePic(String username, String picPath) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setProfilePicPath(picPath);
        userRepository.save(user);
    }

    /**
     * Get the currently authenticated user from SecurityContext.
     */
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Find all users (for admin purposes).
     */
    public java.util.List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Delete user by ID.
     * Guards against deleting users who have created auctions or placed bids.
     */
    @Transactional
    public void deleteById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check for auctions created by this user
        long auctionCount = auctionRepository.findByCreatedByOrderByCreatedAtDesc(user).size();
        if (auctionCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete user \"" + user.getUsername() + "\" — they have " + auctionCount +
                    " auction(s). Deactivate the account instead.");
        }

        // Check for bids placed by this user
        long bidCount = bidRepository.countByBidderId(id);
        if (bidCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete user \"" + user.getUsername() + "\" — they have " + bidCount +
                    " bid(s). Deactivate the account instead.");
        }

        userRepository.deleteById(id);
    }
}
