package com.soma.server.service;

import com.soma.server.entity.User;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.repository.SpotifyUserDetailsRepository;
import com.soma.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.Image;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final SpotifyUserDetailsRepository spotifyUserDetailsRepository;

    @Transactional
    public com.soma.server.entity.User saveOrUpdateUser(se.michaelthelin.spotify.model_objects.specification.User spotifyUser, String accessToken, String refreshToken) {
        Optional<com.soma.server.entity.User> userOptional = userRepository.findByEmail(spotifyUser.getEmail());

        com.soma.server.entity.User appUser;
        if (userOptional.isEmpty()) {
            appUser = new com.soma.server.entity.User();
            appUser.setEmail(spotifyUser.getEmail());
            appUser.setUsername(spotifyUser.getDisplayName());
            appUser.setPassword("spotify-user");
        } else {
            appUser = userOptional.get();
        }

        SpotifyUserDetails details = new SpotifyUserDetails();
        details.setSpotifyUserId(spotifyUser.getId());
        details.setDisplayName(spotifyUser.getDisplayName());
        details.setEmail(spotifyUser.getEmail());
        details.setProductType(spotifyUser.getProduct().toString());
        details.setCountry(spotifyUser.getCountry().toString());
        details.setAccessToken(accessToken);
        details.setRefreshToken(refreshToken);

        Image[] images = spotifyUser.getImages();
        if (images != null && images.length > 0) {
            details.setAvatarUrl(images[0].getUrl());
        }

        appUser.addSpotifyDetails(details);
        return userRepository.save(appUser);
    }

    public Optional<SpotifyUserDetails> getSpotifyUserDetails(String spotifyUserId) {
        return spotifyUserDetailsRepository.findBySpotifyUserId(spotifyUserId);
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
        details.setProductType(spotifyUser.getProduct().toString());
        details.setCountry(spotifyUser.getCountry().toString());
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
    }
}