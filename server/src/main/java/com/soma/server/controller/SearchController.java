package com.soma.server.controller;

import com.soma.server.dto.SearchResultDTO;
import com.soma.server.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for searching tracks, artists, and albums via Spotify API.
 */
@RestController
@RequestMapping("/soma/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search for tracks, artists, albums, and playlists")
public class SearchController {

    private final SearchService searchService;

    @Operation(
        summary = "Search for content",
        description = "Search Spotify catalog for tracks, artists, albums, or playlists"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned"),
        @ApiResponse(responseCode = "400", description = "Invalid search query"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping
    public ResponseEntity<SearchResultDTO> search(
            @Parameter(description = "Search query") 
            @RequestParam String query,
            @Parameter(description = "Spotify user ID for authentication") 
            @RequestParam String userId,
            @Parameter(description = "Type of content to search: track, artist, album, playlist, or all") 
            @RequestParam(defaultValue = "all") String type,
            @Parameter(description = "Maximum number of results (1-50)") 
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Offset for pagination") 
            @RequestParam(defaultValue = "0") int offset) {
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate limit
        limit = Math.max(1, Math.min(50, limit));
        offset = Math.max(0, offset);
        
        return searchService.search(query, userId, type, limit, offset)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Search for tracks",
        description = "Search Spotify catalog specifically for tracks"
    )
    @GetMapping("/tracks")
    public ResponseEntity<SearchResultDTO> searchTracks(
            @Parameter(description = "Search query") 
            @RequestParam String query,
            @Parameter(description = "Spotify user ID") 
            @RequestParam String userId,
            @Parameter(description = "Maximum results") 
            @RequestParam(defaultValue = "20") int limit) {
        
        return searchService.search(query, userId, "track", limit, 0)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Search for artists",
        description = "Search Spotify catalog specifically for artists"
    )
    @GetMapping("/artists")
    public ResponseEntity<SearchResultDTO> searchArtists(
            @Parameter(description = "Search query") 
            @RequestParam String query,
            @Parameter(description = "Spotify user ID") 
            @RequestParam String userId,
            @Parameter(description = "Maximum results") 
            @RequestParam(defaultValue = "20") int limit) {
        
        return searchService.search(query, userId, "artist", limit, 0)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Search for albums",
        description = "Search Spotify catalog specifically for albums"
    )
    @GetMapping("/albums")
    public ResponseEntity<SearchResultDTO> searchAlbums(
            @Parameter(description = "Search query") 
            @RequestParam String query,
            @Parameter(description = "Spotify user ID") 
            @RequestParam String userId,
            @Parameter(description = "Maximum results") 
            @RequestParam(defaultValue = "20") int limit) {
        
        return searchService.search(query, userId, "album", limit, 0)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Search for playlists",
        description = "Search Spotify catalog specifically for playlists"
    )
    @GetMapping("/playlists")
    public ResponseEntity<SearchResultDTO> searchPlaylists(
            @Parameter(description = "Search query") 
            @RequestParam String query,
            @Parameter(description = "Spotify user ID") 
            @RequestParam String userId,
            @Parameter(description = "Maximum results") 
            @RequestParam(defaultValue = "20") int limit) {
        
        return searchService.search(query, userId, "playlist", limit, 0)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

