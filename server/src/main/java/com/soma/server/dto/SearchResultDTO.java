package com.soma.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for search results from Spotify API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search results from Spotify")
public class SearchResultDTO {

    @Schema(description = "Original search query")
    private String query;
    
    @Schema(description = "Total results found")
    private int totalResults;
    
    @Schema(description = "List of matching tracks")
    private List<TrackItem> tracks;
    
    @Schema(description = "List of matching artists")
    private List<ArtistItem> artists;
    
    @Schema(description = "List of matching albums")
    private List<AlbumItem> albums;
    
    @Schema(description = "List of matching playlists")
    private List<PlaylistItem> playlists;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Track search result item")
    public static class TrackItem {
        @Schema(description = "Spotify track ID")
        private String id;
        
        @Schema(description = "Track name")
        private String name;
        
        @Schema(description = "Artist names")
        private List<String> artists;
        
        @Schema(description = "Album name")
        private String album;
        
        @Schema(description = "Album cover image URL")
        private String imageUrl;
        
        @Schema(description = "Duration in milliseconds")
        private int durationMs;
        
        @Schema(description = "Track preview URL (30 sec)")
        private String previewUrl;
        
        @Schema(description = "Spotify track URL")
        private String spotifyUrl;
        
        @Schema(description = "Whether track is explicit")
        private boolean explicit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Artist search result item")
    public static class ArtistItem {
        @Schema(description = "Spotify artist ID")
        private String id;
        
        @Schema(description = "Artist name")
        private String name;
        
        @Schema(description = "Artist image URL")
        private String imageUrl;
        
        @Schema(description = "Artist genres")
        private List<String> genres;
        
        @Schema(description = "Follower count")
        private int followers;
        
        @Schema(description = "Popularity score (0-100)")
        private int popularity;
        
        @Schema(description = "Spotify artist URL")
        private String spotifyUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Album search result item")
    public static class AlbumItem {
        @Schema(description = "Spotify album ID")
        private String id;
        
        @Schema(description = "Album name")
        private String name;
        
        @Schema(description = "Artist names")
        private List<String> artists;
        
        @Schema(description = "Album cover image URL")
        private String imageUrl;
        
        @Schema(description = "Release date")
        private String releaseDate;
        
        @Schema(description = "Total tracks")
        private int totalTracks;
        
        @Schema(description = "Album type: album, single, compilation")
        private String albumType;
        
        @Schema(description = "Spotify album URL")
        private String spotifyUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Playlist search result item")
    public static class PlaylistItem {
        @Schema(description = "Spotify playlist ID")
        private String id;
        
        @Schema(description = "Playlist name")
        private String name;
        
        @Schema(description = "Playlist description")
        private String description;
        
        @Schema(description = "Owner display name")
        private String owner;
        
        @Schema(description = "Playlist cover image URL")
        private String imageUrl;
        
        @Schema(description = "Total tracks")
        private int totalTracks;
        
        @Schema(description = "Whether playlist is public")
        private boolean isPublic;
        
        @Schema(description = "Spotify playlist URL")
        private String spotifyUrl;
    }
}

