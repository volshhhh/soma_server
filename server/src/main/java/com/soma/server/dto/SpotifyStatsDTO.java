package com.soma.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Comprehensive Spotify listening statistics")
public class SpotifyStatsDTO {
    
    @Schema(description = "List of top artists")
    private List<TopArtist> topArtists;
    
    @Schema(description = "List of top tracks")
    private List<TopTrack> topTracks;
    
    @Schema(description = "Genre statistics")
    private List<GenreStat> topGenres;
    
    @Schema(description = "Overall listening statistics")
    private ListeningStats listeningStats;
    
    @Schema(description = "Time range for the statistics", 
            example = "medium_term",
            allowableValues = {"short_term", "medium_term", "long_term"})
    private String timeRange;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopArtist {
        private String id;
        private String name;
        private String imageUrl;
        private int popularity;
        private List<String> genres;
        private int rank;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopTrack {
        private String id;
        private String name;
        private String artistName;
        private String albumName;
        private String albumImageUrl;
        private int durationMs;
        private int popularity;
        private int rank;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenreStat {
        private String genre;
        private int count;
        private double percentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListeningStats {
        private int totalPlaylists;
        private int totalSavedTracks;
        private int totalFollowedArtists;
        private int averageTrackPopularity;
        private int averageArtistPopularity;
    }
}

