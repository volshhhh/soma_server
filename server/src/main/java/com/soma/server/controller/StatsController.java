package com.soma.server.controller;

import com.soma.server.dto.SpotifyStatsDTO;
import com.soma.server.dto.TransferHistoryDTO;
import com.soma.server.service.SpotifyStatsService;
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
@RequestMapping("/soma/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Music listening statistics and analytics")
public class StatsController {
    
    private final SpotifyStatsService spotifyStatsService;
    
    @Operation(
        summary = "Get user statistics",
        description = "Retrieves comprehensive listening statistics including top artists, tracks, genres, and overall stats"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<SpotifyStatsDTO> getStats(
            @Parameter(description = "Spotify user ID")
            @PathVariable String userId,
            @Parameter(description = "Time range: short_term (4 weeks), medium_term (6 months), long_term (all time)")
            @RequestParam(defaultValue = "medium_term") String timeRange) {
        
        if (!isValidTimeRange(timeRange)) {
            timeRange = "medium_term";
        }
        
        Optional<SpotifyStatsDTO> stats = spotifyStatsService.getStats(userId, timeRange);
        return stats.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Get transfer history",
        description = "Retrieves the history of playlist transfers for the user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<TransferHistoryDTO>> getTransferHistory(
            @Parameter(description = "Spotify user ID")
            @PathVariable String userId) {
        
        List<TransferHistoryDTO> history = spotifyStatsService.getTransferHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    @Operation(
        summary = "Get top artists",
        description = "Retrieves the user's top artists for the specified time range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top artists retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/top-artists")
    public ResponseEntity<?> getTopArtists(
            @Parameter(description = "Spotify user ID")
            @PathVariable String userId,
            @Parameter(description = "Time range: short_term, medium_term, long_term")
            @RequestParam(defaultValue = "medium_term") String timeRange) {
        
        if (!isValidTimeRange(timeRange)) {
            timeRange = "medium_term";
        }
        
        Optional<SpotifyStatsDTO> stats = spotifyStatsService.getStats(userId, timeRange);
        return stats.map(s -> ResponseEntity.ok(s.getTopArtists()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Get top tracks",
        description = "Retrieves the user's top tracks for the specified time range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top tracks retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/top-tracks")
    public ResponseEntity<?> getTopTracks(
            @Parameter(description = "Spotify user ID")
            @PathVariable String userId,
            @Parameter(description = "Time range: short_term, medium_term, long_term")
            @RequestParam(defaultValue = "medium_term") String timeRange) {
        
        if (!isValidTimeRange(timeRange)) {
            timeRange = "medium_term";
        }
        
        Optional<SpotifyStatsDTO> stats = spotifyStatsService.getStats(userId, timeRange);
        return stats.map(s -> ResponseEntity.ok(s.getTopTracks()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @Operation(
        summary = "Get genre statistics",
        description = "Retrieves the user's top genres based on their listening history"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Genre statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/genres")
    public ResponseEntity<?> getGenreStats(
            @Parameter(description = "Spotify user ID")
            @PathVariable String userId,
            @Parameter(description = "Time range: short_term, medium_term, long_term")
            @RequestParam(defaultValue = "medium_term") String timeRange) {
        
        if (!isValidTimeRange(timeRange)) {
            timeRange = "medium_term";
        }
        
        Optional<SpotifyStatsDTO> stats = spotifyStatsService.getStats(userId, timeRange);
        return stats.map(s -> ResponseEntity.ok(s.getTopGenres()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    private boolean isValidTimeRange(String timeRange) {
        return "short_term".equals(timeRange) 
                || "medium_term".equals(timeRange) 
                || "long_term".equals(timeRange);
    }
}
