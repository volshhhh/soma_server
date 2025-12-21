package com.soma.server.controller;

import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.soma.server.config.SpotifyConfig;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.service.TransferService;
import com.soma.server.service.UserService;
import com.soma.server.repository.PlaylistTransferRepository;
import com.soma.server.dto.TransferHistoryDTO;

import jakarta.servlet.http.HttpServletResponse;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

@RestController
@RequestMapping("/soma/api")
@PropertySource("classpath:application.properties")
@Tag(name = "Spotify", description = "Spotify integration and playlist operations")
public class SpotifyController {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    @Value("${spotify.client-id}")
    private String clientID;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final SpotifyConfig spotifyConfig;
    private final TransferService transferService;
    private final PlaylistTransferRepository playlistTransferRepository;
    
    private UserService userService;
    
    // Store pending Spotify connection requests (userId -> state)
    private final java.util.concurrent.ConcurrentHashMap<String, Long> pendingConnections = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public SpotifyController(SpotifyConfig spotifyConfig, TransferService transferService, PlaylistTransferRepository playlistTransferRepository) {
        this.spotifyConfig = spotifyConfig;
        this.transferService = transferService;
        this.playlistTransferRepository = playlistTransferRepository;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Operation(
        summary = "Get Spotify login URL",
        description = "Returns the Spotify OAuth authorization URL. Redirect users to this URL to start the OAuth flow."
    )
    @ApiResponse(responseCode = "200", description = "Spotify authorization URL")
    @GetMapping({"/login", "/login-with-spotify"})
    public String login() {
        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();
        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope("user-read-private user-read-email user-top-read user-library-read user-library-modify user-follow-read playlist-read-private playlist-read-collaborative playlist-modify-public playlist-modify-private")
                .show_dialog(true)
                .build();
        return request.execute().toString();
    }
    
    @Operation(
        summary = "Connect Spotify account to existing user",
        description = "Initiates Spotify OAuth flow to link a Spotify account to an existing SOMA user. Use this for users who registered with email/password."
    )
    @ApiResponse(responseCode = "302", description = "Redirect to Spotify authorization page")
    @GetMapping("/connect-spotify")
    public void connectSpotify(
            @Parameter(description = "Internal SOMA user ID") @RequestParam("userId") Long userId, 
            HttpServletResponse response) throws java.io.IOException {
        logger.info("Connect Spotify initiated for user: {}", userId);
        
        // Generate a unique state to track this connection request
        String state = java.util.UUID.randomUUID().toString();
        pendingConnections.put(state, userId);
        
        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();
        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope("user-read-private user-read-email user-top-read user-library-read user-library-modify user-follow-read playlist-read-private playlist-read-collaborative playlist-modify-public playlist-modify-private")
                .state(state)
                .show_dialog(true)
                .build();
        
        response.sendRedirect(request.execute().toString());
    }
    
    @Operation(summary = "Connect Spotify callback - links Spotify to existing user")
    @GetMapping("/connect-callback")
    public void connectCallback(
            @RequestParam("code") String userCode,
            @RequestParam("state") String state,
            HttpServletResponse response) throws java.io.IOException {
        logger.info("=== CONNECT CALLBACK START ===");
        
        Long userId = pendingConnections.remove(state);
        if (userId == null) {
            logger.error("No pending connection found for state: {}", state);
            response.sendRedirect("http://localhost:5173/profile?error=invalid_state");
            return;
        }
        
        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();
        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(userCode).build();

        try {
            final AuthorizationCodeCredentials credentials = authorizationCodeRequest.execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            final GetCurrentUsersProfileRequest getCurrentUsersProfile = spotifyApi.getCurrentUsersProfile().build();
            User spotifyUser = getCurrentUsersProfile.execute();

            userService.linkSpotifyAccount(userId, spotifyUser, credentials.getAccessToken(), credentials.getRefreshToken());
            
            logger.info("Successfully linked Spotify account {} to user {}", spotifyUser.getId(), userId);
            response.sendRedirect("http://localhost:5173/profile?id=" + userId + "&spotify_connected=true");
        } catch (IllegalStateException e) {
            logger.error("Spotify account already linked: {}", e.getMessage());
            response.sendRedirect("http://localhost:5173/profile?id=" + userId + "&error=already_linked");
        } catch (Exception e) {
            logger.error("Error in connect callback: {}", e.getMessage());
            response.sendRedirect("http://localhost:5173/profile?id=" + userId + "&error=connection_failed");
        }
    }

    @Operation(summary = "OAuth callback endpoint")
    @GetMapping("/callback")
    public void getCode(@RequestParam("code") String userCode, HttpServletResponse response) throws java.io.IOException {
        logger.info("=== CALLBACK START ===");
        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();
        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(userCode).build();

        try {
            final AuthorizationCodeCredentials credentials = authorizationCodeRequest.execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            final GetCurrentUsersProfileRequest getCurrentUsersProfile = spotifyApi.getCurrentUsersProfile().build();
            User user = getCurrentUsersProfile.execute();

            try {
                userService.saveOrUpdateUser(user, credentials.getAccessToken(), credentials.getRefreshToken());
            } catch (Exception dbError) {
                logger.warn("Database save failed: {}", dbError.getMessage());
            }

            response.sendRedirect("http://localhost:5173/home?id=" + user.getId());
        } catch (Exception e) {
            logger.error("Error in callback: {}", e.getMessage());
            response.sendRedirect("http://localhost:5173/login?error=auth_failed");
        }
    }

    @Operation(summary = "Get home page welcome message")
    @GetMapping("home")
    public String home(@RequestParam("id") String userId) {
        // First try to find by Spotify user ID
        Optional<SpotifyUserDetails> spotifyDetails = userService.getSpotifyUserDetails(userId);
        if (spotifyDetails.isPresent()) {
            String displayName = spotifyDetails.get().getDisplayName();
            return "Welcome, " + (displayName != null ? displayName : "User");
        }
        
        // Try to find by internal user ID (numeric)
        try {
            Long internalId = Long.parseLong(userId);
            Optional<com.soma.server.entity.User> user = userService.findById(internalId);
            if (user.isPresent()) {
                return "Welcome, " + user.get().getUsername();
            }
        } catch (NumberFormatException e) {
            // Not a numeric ID, ignore
        }
        
        return "Welcome, User";
    }

    @Operation(summary = "Get user avatar URL")
    @GetMapping("user-avatar")
    public ResponseEntity<String> getUserAvatar(@RequestParam("id") String userId) {
        try {
            // First try to find by Spotify user ID
            Optional<SpotifyUserDetails> spotifyDetails = userService.getSpotifyUserDetails(userId);
            if (spotifyDetails.isPresent()) {
                String avatarUrl = spotifyDetails.get().getAvatarUrl();
                return ResponseEntity.ok(avatarUrl != null ? avatarUrl : "");
            }
            
            // Try to find by internal user ID (numeric) - check if they have Spotify linked
            try {
                Long internalId = Long.parseLong(userId);
                Optional<SpotifyUserDetails> linkedSpotify = userService.getSpotifyDetailsByUserId(internalId);
                if (linkedSpotify.isPresent()) {
                    String avatarUrl = linkedSpotify.get().getAvatarUrl();
                    return ResponseEntity.ok(avatarUrl != null ? avatarUrl : "");
                }
            } catch (NumberFormatException e) {
                // Not a numeric ID, ignore
            }
            
            // No avatar found - return empty string (frontend will show letter placeholder)
            return ResponseEntity.ok("");
        } catch (Exception e) {
            return ResponseEntity.ok("");
        }
    }

    @Operation(
        summary = "Create a new playlist from Yandex Music",
        description = "Transfers tracks from a Yandex Music playlist to a new Spotify playlist. The transfer runs asynchronously; use the returned transferId to track progress."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer started successfully"),
        @ApiResponse(responseCode = "400", description = "User not found or invalid request")
    })
    @PostMapping("add-playlist")
    public ResponseEntity<Map<String, Object>> addPlaylist(
            @Parameter(description = "Name for the new Spotify playlist") @RequestParam("playlistName") String playlistName,
            @Parameter(description = "Public Yandex Music playlist URL") @RequestParam("playlistLink") String playlistLink, 
            @Parameter(description = "Spotify user ID") @RequestParam("userId") String userId) {
        
        logger.info("Received add-playlist request: playlistName={}, link={}, userId={}", playlistName, playlistLink, userId);

        Optional<SpotifyUserDetails> userDetailsOptional = userService.getSpotifyUserDetails(userId);
        if (userDetailsOptional.isEmpty()) {
            logger.warn("User not found for add-playlist: userId={}", userId);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }
        SpotifyUserDetails userDetails = userDetailsOptional.get();

        Long transferId = transferService.createTransferRecord(userDetails, playlistLink, null);
        logger.info("Created transfer record: transferId={}, user={}", transferId, userDetails.getDisplayName());
        
        // Start async process
        transferService.processTransfer(transferId, userDetails.getAccessToken(), playlistName, true, null);
        logger.info("Started async transfer process for transferId={}", transferId);

        return ResponseEntity.ok(Map.of("success", true, "transferId", transferId));
    }

    @Operation(
        summary = "Add tracks to existing playlist",
        description = "Transfers tracks from a Yandex Music playlist to an existing Spotify playlist. The transfer runs asynchronously; use the returned transferId to track progress."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer started successfully"),
        @ApiResponse(responseCode = "400", description = "User not found or invalid Spotify playlist link")
    })
    @PostMapping("add-to-existing")
    public ResponseEntity<Map<String, Object>> addToExisting(
            @Parameter(description = "Public Yandex Music playlist URL") @RequestParam("yandexLink") String yandexPlaylistLink,
            @Parameter(description = "Spotify playlist URL to add tracks to") @RequestParam("spotifyLink") String spotifyPlaylistLink,
            @Parameter(description = "Spotify user ID") @RequestParam("userId") String userId) {
        
        logger.info("Received add-to-existing request: yandexLink={}, spotifyLink={}, userId={}", yandexPlaylistLink, spotifyPlaylistLink, userId);

        Optional<SpotifyUserDetails> userDetailsOptional = userService.getSpotifyUserDetails(userId);
        if (userDetailsOptional.isEmpty()) {
            logger.warn("User not found for add-to-existing: userId={}", userId);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }
        SpotifyUserDetails userDetails = userDetailsOptional.get();
        
        String playlistId = extractPlaylistId(spotifyPlaylistLink);
        if (playlistId.isEmpty()) {
             logger.warn("Invalid Spotify playlist link provided: {}", spotifyPlaylistLink);
             return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid Spotify Playlist Link"));
        }

        Long transferId = transferService.createTransferRecord(userDetails, yandexPlaylistLink, spotifyPlaylistLink);
        logger.info("Created transfer record for existing playlist: transferId={}, playlistId={}", transferId, playlistId);
        
        // Start async process
        transferService.processTransfer(transferId, userDetails.getAccessToken(), null, false, playlistId);
        logger.info("Started async transfer process (existing) for transferId={}", transferId);

        return ResponseEntity.ok(Map.of("success", true, "transferId", transferId));
    }

    @Operation(
        summary = "Get transfer progress",
        description = "Returns the current status and progress of a playlist transfer operation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer status returned"),
        @ApiResponse(responseCode = "404", description = "Transfer not found")
    })
    @GetMapping("/transfer/{transferId}/progress")
    public ResponseEntity<TransferHistoryDTO> getTransferProgress(
            @Parameter(description = "Transfer ID returned from add-playlist or add-to-existing") @PathVariable Long transferId) {
        return playlistTransferRepository.findById(transferId)
                .map(t -> ResponseEntity.ok(TransferHistoryDTO.builder()
                        .id(t.getId())
                        .yandexPlaylistLink(t.getYandexPlaylistLink())
                        .spotifyPlaylistLink(t.getSpotifyPlaylistLink())
                        .status(t.getStatus().toString())
                        .trackCount(t.getTrackCount())
                        .transferredCount(t.getTransferredCount())
                        .createdAt(t.getCreatedAt())
                        .errorMessage(t.getErrorMessage())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }
    
    private String extractPlaylistId(String url) {
        String prefix = "/playlist/";
        int startIndex = url.indexOf(prefix);
        if (startIndex == -1) return "";
        startIndex += prefix.length();
        int endIndex = url.indexOf("?", startIndex);
        if (endIndex == -1) endIndex = url.length();
        return url.substring(startIndex, endIndex);
    }
}
