package com.soma.server.controller;


import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soma.server.config.SpotifyConfig;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.service.UserService;

import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import jakarta.servlet.http.HttpServletResponse;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.special.SnapshotResult;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.CreatePlaylistRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import org.apache.hc.core5.http.ParseException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/soma/api")
@PropertySource("classpath:application.properties")
public class SpotifyController {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    @Value("${spotify.client-id}")
    private String clientID;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${custom.server.ip}")
    private String customIP;

    private final SpotifyConfig spotifyConfig;

    private UserService userService;

    private String name = "";

    @Autowired
    public SpotifyController(SpotifyConfig spotifyConfig) {
        this.spotifyConfig = spotifyConfig;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login-with-spotify")
    public String login() {
        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();

        AuthorizationCodeUriRequest request = spotifyApi.authorizationCodeUri()
                .scope(
                        "user-read-private " +
                                "user-top-read " +
                                "user-library-read " +
                                "user-library-modify " +
                                "playlist-read-private " +
                                "playlist-read-collaborative " +
                                "playlist-modify-public " +
                                "playlist-modify-private"
                )
                .show_dialog(true)
                .build();
        URI uri = request.execute();

        return uri.toString();
    }

    @GetMapping("/callback")
    public void getCode(@RequestParam("code") String userCode, HttpServletResponse response) throws IOException {

        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(userCode)
                .build();
        User user;

        try {
            final AuthorizationCodeCredentials credentials = authorizationCodeRequest.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            final GetCurrentUsersProfileRequest getCurrentUsersProfile = spotifyApi.getCurrentUsersProfile().build();
            user = getCurrentUsersProfile.execute();

            name = user.getDisplayName();

            userService.updateUser(user, credentials.getAccessToken(), credentials.getRefreshToken());

        // Redirect to frontend with user ID
        response.sendRedirect("http://localhost:8080/soma/api/home?id=" + user.getId());

        } catch (Exception e) {
            logger.error("Error in callback while processing code {}: {}", userCode, e.getMessage(), e);
        // Redirect to frontend error page
            response.sendRedirect("http://localhost:5173/error");
        }
    }

    @GetMapping(value = "home")
    public String home(@RequestParam("id") String userId) {
        try {
            return "Welcome, " + name;
        } catch (Exception e) {
            logger.error("Error loading home page for user {}: {}", userId, e.getMessage(), e);;
        }

        return null;
    }
}