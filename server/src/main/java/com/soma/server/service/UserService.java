package com.soma.server.service;

import com.soma.server.entity.User;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.repository.SpotifyUserDetailsRepository;
import com.soma.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.Image;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final SpotifyUserDetailsRepository spotifyUserDetailsRepository;

    @Transactional
    public com.soma.server.entity.User saveOrUpdateUser(se.michaelthelin.spotify.model_objects.specification.User spotifyUser, String accessToken, String refreshToken) {
        log.info("saveOrUpdateUser called for Spotify user: {}", spotifyUser.getId());
        
        // First, try to find existing user by Spotify ID (preferred)
        Optional<SpotifyUserDetails> existingDetails = spotifyUserDetailsRepository.findBySpotifyUserId(spotifyUser.getId());
        
        com.soma.server.entity.User appUser;
        if (existingDetails.isPresent()) {
            // User exists, update their details
            log.info("Found existing user by Spotify ID");
            appUser = existingDetails.get().getUser();
        } else {
            // Try to find by email if email exists
            String email = spotifyUser.getEmail();
            log.info("No existing user found, checking email: {}", email);
            if (email != null && !email.isEmpty()) {
                Optional<com.soma.server.entity.User> userByEmail = userRepository.findByEmail(email);
                if (userByEmail.isPresent()) {
                    log.info("Found existing user by email");
                    appUser = userByEmail.get();
                } else {
                    log.info("Creating new user (email exists but user not found)");
                    appUser = createNewUser(spotifyUser);
                }
            } else {
                log.info("Creating new user (no email available)");
                appUser = createNewUser(spotifyUser);
            }
        }

        // Get existing details or create new
        SpotifyUserDetails details = spotifyUserDetailsRepository.findBySpotifyUserId(spotifyUser.getId())
                .orElse(new SpotifyUserDetails());
        
        details.setSpotifyUserId(spotifyUser.getId());
        details.setDisplayName(spotifyUser.getDisplayName());
        details.setEmail(spotifyUser.getEmail());
        
        // Handle potential null product/country
        if (spotifyUser.getProduct() != null) {
            details.setProductType(spotifyUser.getProduct().toString());
        }
        if (spotifyUser.getCountry() != null) {
            details.setCountry(spotifyUser.getCountry().toString());
        }
        
        details.setAccessToken(accessToken);
        details.setRefreshToken(refreshToken);

        Image[] images = spotifyUser.getImages();
        if (images != null && images.length > 0) {
            details.setAvatarUrl(images[0].getUrl());
        }

        // Only add to user if it's a new details object
        if (details.getId() == null) {
            appUser.addSpotifyDetails(details);
        }
        
        return userRepository.save(appUser);
    }
    
    private com.soma.server.entity.User createNewUser(se.michaelthelin.spotify.model_objects.specification.User spotifyUser) {
        com.soma.server.entity.User appUser = new com.soma.server.entity.User();
        
        // Use Spotify ID as username to ensure uniqueness
        // Display name will be stored in SpotifyUserDetails
        String username = "spotify_" + spotifyUser.getId();
        appUser.setUsername(username);
        
        // Email can be null for Spotify-only auth
        appUser.setEmail(spotifyUser.getEmail());
        
        // Password is optional for OAuth-only users
        appUser.setPassword(null);
        
        log.info("Creating new user with username: {}, email: {}", username, spotifyUser.getEmail());
        
        return appUser;
    }

    public Optional<SpotifyUserDetails> getSpotifyUserDetails(String spotifyUserId) {
        return spotifyUserDetailsRepository.findBySpotifyUserId(spotifyUserId);
    }
    
    public Optional<SpotifyUserDetails> getSpotifyUserDetailsWithUser(String spotifyUserId) {
        return spotifyUserDetailsRepository.findBySpotifyUserIdWithUser(spotifyUserId);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<SpotifyUserDetails> getSpotifyDetailsByUserId(Long userId) {
        return spotifyUserDetailsRepository.findByUserId(userId);
    }

    @Transactional
    public void linkSpotifyAccount(Long userId, se.michaelthelin.spotify.model_objects.specification.User spotifyUser, String accessToken, String refreshToken) {
        User appUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if this Spotify account is already linked to another user
        spotifyUserDetailsRepository.findBySpotifyUserId(spotifyUser.getId()).ifPresent(details -> {
            if (!details.getUser().getId().equals(userId)) {
                throw new IllegalStateException("This Spotify account is already linked to another user.");
            }
        });

        SpotifyUserDetails details = appUser.getAllUserDetails().stream()
                .filter(d -> d instanceof SpotifyUserDetails)
                .map(d -> (SpotifyUserDetails) d)
                .findFirst()
                .orElse(new SpotifyUserDetails());


        details.setSpotifyUserId(spotifyUser.getId());
        details.setDisplayName(spotifyUser.getDisplayName());
        details.setEmail(spotifyUser.getEmail());
        
        // Handle potential null product/country
        if (spotifyUser.getProduct() != null) {
        details.setProductType(spotifyUser.getProduct().toString());
        }
        if (spotifyUser.getCountry() != null) {
        details.setCountry(spotifyUser.getCountry().toString());
        }
        
        details.setAccessToken(accessToken);
        details.setRefreshToken(refreshToken);

        Image[] images = spotifyUser.getImages();
        if (images != null && images.length > 0) {
            details.setAvatarUrl(images[0].getUrl());
        }

        // If it's a new details object, add it to the user's list
        if (details.getId() == null) {
            appUser.addSpotifyDetails(details);
        }

        userRepository.save(appUser);
        log.info("Successfully linked Spotify account {} to user {}", spotifyUser.getId(), userId);
    }
}