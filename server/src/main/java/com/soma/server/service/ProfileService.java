package com.soma.server.service;

import com.soma.server.dto.ProfileDTO;
import com.soma.server.dto.ProfileUpdateRequest;
import com.soma.server.entity.PlaylistTransfer;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.entity.User;
import com.soma.server.repository.PlaylistTransferRepository;
import com.soma.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    
    private final UserRepository userRepository;
    private final UserService userService;
    private final PlaylistTransferRepository playlistTransferRepository;
    
    @Transactional(readOnly = true)
    public Optional<ProfileDTO> getProfileBySpotifyId(String spotifyUserId) {
        try {
            Optional<SpotifyUserDetails> spotifyDetails = userService.getSpotifyUserDetailsWithUser(spotifyUserId);
            
            if (spotifyDetails.isEmpty()) {
                log.warn("Spotify user details not found for ID: {}", spotifyUserId);
                return Optional.empty();
            }
            
            SpotifyUserDetails details = spotifyDetails.get();
            User user = details.getUser();
            
            List<ProfileDTO.ConnectedServiceDTO> connectedServices = new ArrayList<>();
            
            // Add Spotify connection
            connectedServices.add(ProfileDTO.ConnectedServiceDTO.builder()
                    .serviceName("Spotify")
                    .serviceUserId(details.getSpotifyUserId())
                    .displayName(details.getDisplayName())
                    .avatarUrl(details.getAvatarUrl())
                    .connected(true)
                    .connectedAt(LocalDateTime.now()) // Ideally this should be from DB
                    .build());
            
            // Handle case where User might be null (orphaned Spotify details)
            if (user == null) {
                log.warn("Spotify details found but no associated User for ID: {}, returning profile from Spotify data", spotifyUserId);
                return Optional.of(ProfileDTO.builder()
                        .id(0L)
                        .username(details.getDisplayName())
                        .email(details.getEmail())
                        .role("USER")
                        .avatarUrl(details.getAvatarUrl())
                        .displayName(details.getDisplayName())
                        .connectedServices(connectedServices)
                        .createdAt(LocalDateTime.now())
                        .lastLogin(LocalDateTime.now())
                        .build());
            }
            
            return Optional.of(ProfileDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername() != null ? user.getUsername() : details.getDisplayName())
                    .email(user.getEmail() != null ? user.getEmail() : details.getEmail())
                    .role(user.getRole() != null ? user.getRole().toString() : "USER")
                    .avatarUrl(details.getAvatarUrl())
                    .displayName(details.getDisplayName())
                    .connectedServices(connectedServices)
                    .createdAt(LocalDateTime.now()) // Placeholder
                    .lastLogin(LocalDateTime.now()) // Placeholder
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving profile for Spotify ID: {}", spotifyUserId, e);
            throw e;
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<ProfileDTO> getProfileByUserId(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        
        User user = userOpt.get();
        List<ProfileDTO.ConnectedServiceDTO> connectedServices = new ArrayList<>();
        String avatarUrl = null;
        String displayName = user.getUsername();
        
        // Check for connected services in allUserDetails
        for (var detail : user.getAllUserDetails()) {
            if (detail instanceof SpotifyUserDetails spotifyDetails) {
                avatarUrl = spotifyDetails.getAvatarUrl();
                displayName = spotifyDetails.getDisplayName();
                
                connectedServices.add(ProfileDTO.ConnectedServiceDTO.builder()
                        .serviceName("Spotify")
                        .serviceUserId(spotifyDetails.getSpotifyUserId())
                        .displayName(spotifyDetails.getDisplayName())
                        .avatarUrl(spotifyDetails.getAvatarUrl())
                        .connected(true)
                        .connectedAt(LocalDateTime.now())
                        .build());
            }
        }
        
        return Optional.of(ProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().toString() : "USER")
                .avatarUrl(avatarUrl)
                .displayName(displayName)
                .connectedServices(connectedServices)
                .createdAt(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .build());
    }
    
    public List<PlaylistTransfer> getTransferHistory(String spotifyUserId) {
        Optional<SpotifyUserDetails> details = userService.getSpotifyUserDetails(spotifyUserId);
        if (details.isPresent() && details.get().getUser() != null) {
            return playlistTransferRepository.findByUserIdOrderByCreatedAtDesc(details.get().getUser().getId());
        }
        return Collections.emptyList();
    }
    
    @Transactional
    public Optional<ProfileDTO> updateProfile(Long userId, ProfileUpdateRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        
        User user = userOpt.get();
        
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            user.setUsername(request.getUsername().trim());
        }
        
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.setEmail(request.getEmail().trim());
        }
        
        userRepository.save(user);
        
        return getProfileByUserId(userId);
    }
    
    @Transactional
    public boolean disconnectService(Long userId, String serviceName) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return false;
        }
        
        User user = userOpt.get();
        
        // Find and remove the service
        user.getAllUserDetails().removeIf(detail -> {
            if ("Spotify".equalsIgnoreCase(serviceName) && detail instanceof SpotifyUserDetails) {
                return true;
            }
            return false;
        });
        
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean upgradeToPremium(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRole(User.Role.PREMIUM);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
