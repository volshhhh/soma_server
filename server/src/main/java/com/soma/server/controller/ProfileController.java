package com.soma.server.controller;

import com.soma.server.dto.ProfileDTO;
import com.soma.server.dto.ProfileUpdateRequest;
import com.soma.server.entity.PlaylistTransfer;
import com.soma.server.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/soma/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile management")
public class ProfileController {
    
    private final ProfileService profileService;
    
    @Operation(
        summary = "Get profile by Spotify ID",
        description = "Retrieves the user profile associated with a Spotify user ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile found"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/spotify/{spotifyUserId}")
    public ResponseEntity<ProfileDTO> getProfileBySpotifyId(
            @Parameter(description = "Spotify user ID")
            @PathVariable String spotifyUserId) {
        Optional<ProfileDTO> profile = profileService.getProfileBySpotifyId(spotifyUserId);
        return profile.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Get profile by user ID",
        description = "Retrieves the user profile by internal user ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile found"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileDTO> getProfileByUserId(
            @Parameter(description = "Internal user ID")
            @PathVariable Long userId) {
        Optional<ProfileDTO> profile = profileService.getProfileByUserId(userId);
        return profile.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Get transfer history",
        description = "Retrieves the playlist transfer history for a user"
    )
    @GetMapping("/spotify/{spotifyUserId}/history")
    public ResponseEntity<List<PlaylistTransfer>> getTransferHistory(
            @Parameter(description = "Spotify user ID")
            @PathVariable String spotifyUserId) {
        return ResponseEntity.ok(profileService.getTransferHistory(spotifyUserId));
    }
    
    @Operation(
        summary = "Update user profile",
        description = "Updates the user profile information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/user/{userId}")
    public ResponseEntity<ProfileDTO> updateProfile(
            @Parameter(description = "Internal user ID")
            @PathVariable Long userId,
            @RequestBody ProfileUpdateRequest request) {
        Optional<ProfileDTO> updatedProfile = profileService.updateProfile(userId, request);
        return updatedProfile.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Disconnect a service",
        description = "Disconnects a connected streaming service from the user's account"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service disconnected successfully"),
        @ApiResponse(responseCode = "404", description = "User or service not found")
    })
    @DeleteMapping("/user/{userId}/service/{serviceName}")
    public ResponseEntity<Void> disconnectService(
            @Parameter(description = "Internal user ID")
            @PathVariable Long userId,
            @Parameter(description = "Service name (e.g., Spotify, YandexMusic)")
            @PathVariable String serviceName) {
        boolean success = profileService.disconnectService(userId, serviceName);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "Upgrade user to Premium",
        description = "Upgrades the user's role to PREMIUM"
    )
    @PostMapping("/user/{userId}/upgrade")
    public ResponseEntity<Void> upgradeToPremium(
            @Parameter(description = "Internal user ID")
            @PathVariable Long userId) {
        boolean success = profileService.upgradeToPremium(userId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
