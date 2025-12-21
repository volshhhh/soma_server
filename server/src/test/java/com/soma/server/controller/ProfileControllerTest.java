package com.soma.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soma.server.dto.ProfileDTO;
import com.soma.server.dto.ProfileUpdateRequest;
import com.soma.server.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@DisplayName("ProfileController Tests")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @Test
    @WithMockUser
    @DisplayName("GET /soma/api/profile/spotify/{id} should return profile")
    void testGetProfileBySpotifyId() throws Exception {
        // Given
        String spotifyUserId = "spotify-user-123";
        ProfileDTO profile = ProfileDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .connectedServices(new ArrayList<>())
                .build();

        when(profileService.getProfileBySpotifyId(spotifyUserId))
                .thenReturn(Optional.of(profile));

        // When & Then
        mockMvc.perform(get("/soma/api/profile/spotify/{spotifyUserId}", spotifyUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /soma/api/profile/spotify/{id} should return 404 when not found")
    void testGetProfileBySpotifyId_NotFound() throws Exception {
        // Given
        when(profileService.getProfileBySpotifyId("nonexistent"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/soma/api/profile/spotify/{spotifyUserId}", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /soma/api/profile/user/{id} should update profile")
    void testUpdateProfile() throws Exception {
        // Given
        Long userId = 1L;
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest("newusername", "new@email.com", "New Name");
        ProfileDTO updatedProfile = ProfileDTO.builder()
                .id(userId)
                .username("newusername")
                .email("new@email.com")
                .displayName("New Name")
                .build();

        when(profileService.updateProfile(eq(userId), any(ProfileUpdateRequest.class)))
                .thenReturn(Optional.of(updatedProfile));

        // When & Then
        mockMvc.perform(put("/soma/api/profile/user/{userId}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newusername"))
                .andExpect(jsonPath("$.email").value("new@email.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /soma/api/profile/user/{id}/service/{name} should disconnect service")
    void testDisconnectService() throws Exception {
        // Given
        when(profileService.disconnectService(1L, "Spotify")).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/soma/api/profile/user/{userId}/service/{serviceName}", 1L, "Spotify")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}


