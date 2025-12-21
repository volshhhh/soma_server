package com.soma.server.controller;

import com.soma.server.dto.SpotifyStatsDTO;
import com.soma.server.service.SpotifyStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
@DisplayName("StatsController Tests")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpotifyStatsService spotifyStatsService;

    @Test
    @WithMockUser
    @DisplayName("GET /soma/api/stats/{userId} should return stats")
    void testGetStats() throws Exception {
        // Given
        SpotifyStatsDTO stats = SpotifyStatsDTO.builder()
                .topArtists(Arrays.asList(
                        SpotifyStatsDTO.TopArtist.builder()
                                .id("artist1")
                                .name("Test Artist")
                                .popularity(80)
                                .rank(1)
                                .build()
                ))
                .topTracks(Arrays.asList(
                        SpotifyStatsDTO.TopTrack.builder()
                                .id("track1")
                                .name("Test Track")
                                .artistName("Test Artist")
                                .rank(1)
                                .build()
                ))
                .topGenres(new ArrayList<>())
                .listeningStats(SpotifyStatsDTO.ListeningStats.builder()
                        .totalPlaylists(10)
                        .totalSavedTracks(500)
                        .build())
                .timeRange("medium_term")
                .build();

        when(spotifyStatsService.getStats(anyString(), anyString()))
                .thenReturn(Optional.of(stats));

        // When & Then
        mockMvc.perform(get("/soma/api/stats/{userId}", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange").value("medium_term"))
                .andExpect(jsonPath("$.topArtists[0].name").value("Test Artist"))
                .andExpect(jsonPath("$.topTracks[0].name").value("Test Track"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /soma/api/stats/{userId} should return 404 when not found")
    void testGetStats_NotFound() throws Exception {
        // Given
        when(spotifyStatsService.getStats(anyString(), anyString()))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/soma/api/stats/{userId}", "nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /soma/api/stats/{userId} should use default time range")
    void testGetStats_DefaultTimeRange() throws Exception {
        // Given
        SpotifyStatsDTO stats = SpotifyStatsDTO.builder()
                .timeRange("medium_term")
                .topArtists(new ArrayList<>())
                .topTracks(new ArrayList<>())
                .topGenres(new ArrayList<>())
                .build();

        when(spotifyStatsService.getStats("user123", "medium_term"))
                .thenReturn(Optional.of(stats));

        // When & Then
        mockMvc.perform(get("/soma/api/stats/{userId}", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeRange").value("medium_term"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /soma/api/stats/{userId}/top-artists should return top artists")
    void testGetTopArtists() throws Exception {
        // Given
        SpotifyStatsDTO stats = SpotifyStatsDTO.builder()
                .topArtists(Arrays.asList(
                        SpotifyStatsDTO.TopArtist.builder()
                                .id("artist1")
                                .name("Artist One")
                                .rank(1)
                                .build(),
                        SpotifyStatsDTO.TopArtist.builder()
                                .id("artist2")
                                .name("Artist Two")
                                .rank(2)
                                .build()
                ))
                .build();

        when(spotifyStatsService.getStats(anyString(), anyString()))
                .thenReturn(Optional.of(stats));

        // When & Then
        mockMvc.perform(get("/soma/api/stats/{userId}/top-artists", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Artist One"))
                .andExpect(jsonPath("$[1].name").value("Artist Two"));
    }
}


