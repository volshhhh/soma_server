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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;


@RestController
@RequestMapping("/soma/api/{userId}")
@PropertySource("classpath:application.properties")
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private final SpotifyConfig spotifyConfig;
    private final UserService userService;

    @Autowired
    public MainController(SpotifyConfig spotifyConfig, UserService userService) {
        this.spotifyConfig = spotifyConfig;
        this.userService = userService;
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to Soma";
    }

    @PostMapping("/add-spotify")
    public void addSpotify(@PathVariable Long userId, @RequestBody SpotifyAuthRequest request, HttpServletResponse response, @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        // Basic validation to ensure the authenticated user matches the path variable
        if (userDetails instanceof com.soma.server.entity.User) {
            com.soma.server.entity.User authenticatedUser = (com.soma.server.entity.User) userDetails;
            if (!authenticatedUser.getId().equals(userId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "You can only link an account for yourself.");
                return;
            }
        } else {
            // Handle cases where UserDetails might not be your custom User entity
            // This could be a security risk if not handled properly
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user details type.");
            return;
        }


        SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(request.getCode())
                .build();

        try {
            final AuthorizationCodeCredentials credentials = authorizationCodeRequest.execute();

            spotifyApi.setAccessToken(credentials.getAccessToken());
            spotifyApi.setRefreshToken(credentials.getRefreshToken());

            final GetCurrentUsersProfileRequest getCurrentUsersProfile = spotifyApi.getCurrentUsersProfile().build();
            User spotifyUser = getCurrentUsersProfile.execute();

            userService.linkSpotifyAccount(userId, spotifyUser, credentials.getAccessToken(), credentials.getRefreshToken());

            response.sendRedirect("http://localhost:5173/home"); // Redirect to a generic home page

        } catch (Exception e) {
            logger.error("Error in callback while processing code {}: {}", request.getCode(), e.getMessage(), e);
            response.sendRedirect("http://localhost:5173/error");
        }
    }

    @Getter
    @Setter
    public static class SpotifyAuthRequest {
        private String code;
    }

    

    
}
