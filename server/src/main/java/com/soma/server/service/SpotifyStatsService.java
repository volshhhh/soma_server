package com.soma.server.service;

import com.soma.server.dto.SpotifyStatsDTO;
import com.soma.server.dto.TransferHistoryDTO;
import com.soma.server.entity.PlaylistTransfer;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.repository.PlaylistTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopArtistsRequest;
import se.michaelthelin.spotify.requests.data.personalization.simplified.GetUsersTopTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.follow.GetUsersFollowedArtistsRequest;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyStatsService {
    
    private final UserService userService;
    private final PlaylistTransferRepository playlistTransferRepository;
    
    public Optional<SpotifyStatsDTO> getStats(String spotifyUserId, String timeRange) {
        log.info("Fetching stats for user: {} with timeRange: {}", spotifyUserId, timeRange);
        
        Optional<SpotifyUserDetails> userDetailsOpt = userService.getSpotifyUserDetails(spotifyUserId);
        
        if (userDetailsOpt.isEmpty()) {
            log.warn("No SpotifyUserDetails found for userId: {}", spotifyUserId);
            return Optional.empty();
        }
        
        SpotifyUserDetails userDetails = userDetailsOpt.get();
        log.info("Found user details for: {}, accessToken present: {}", 
                userDetails.getDisplayName(), 
                userDetails.getAccessToken() != null && !userDetails.getAccessToken().isEmpty());
        
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(userDetails.getAccessToken())
                .build();
        
        try {
            List<SpotifyStatsDTO.TopArtist> topArtists = fetchTopArtists(spotifyApi, timeRange);
            List<SpotifyStatsDTO.TopTrack> topTracks = fetchTopTracks(spotifyApi, timeRange);
            List<SpotifyStatsDTO.GenreStat> genreStats = calculateGenreStats(topArtists);
            SpotifyStatsDTO.ListeningStats listeningStats = fetchListeningStats(spotifyApi, topArtists, topTracks);
            
            return Optional.of(SpotifyStatsDTO.builder()
                    .topArtists(topArtists)
                    .topTracks(topTracks)
                    .topGenres(genreStats)
                    .listeningStats(listeningStats)
                    .timeRange(timeRange)
                    .build());
                    
        } catch (Exception e) {
            log.error("Error fetching Spotify stats for user {}: {}", spotifyUserId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    public List<TransferHistoryDTO> getTransferHistory(String spotifyUserId) {
        Optional<SpotifyUserDetails> userDetailsOpt = userService.getSpotifyUserDetails(spotifyUserId);
        
        if (userDetailsOpt.isEmpty() || userDetailsOpt.get().getUser() == null) {
            return Collections.emptyList();
        }
        
        Long internalUserId = userDetailsOpt.get().getUser().getId();
        List<PlaylistTransfer> transfers = playlistTransferRepository.findByUserIdOrderByCreatedAtDesc(internalUserId);
        
        return transfers.stream()
                .map(t -> TransferHistoryDTO.builder()
                        .id(t.getId())
                        .yandexPlaylistLink(t.getYandexPlaylistLink())
                        .spotifyPlaylistLink(t.getSpotifyPlaylistLink())
                        .status(t.getStatus().toString())
                        .trackCount(t.getTrackCount())
                        .createdAt(t.getCreatedAt())
                        .errorMessage(t.getErrorMessage())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SpotifyStatsDTO.TopArtist> fetchTopArtists(SpotifyApi spotifyApi, String timeRange) 
            throws IOException, SpotifyWebApiException, ParseException {
        
        GetUsersTopArtistsRequest request = spotifyApi.getUsersTopArtists()
                .time_range(timeRange)
                .limit(20)
                .build();
        
        Paging<Artist> artistPaging = request.execute();
        List<SpotifyStatsDTO.TopArtist> result = new ArrayList<>();
        
        int rank = 1;
        for (Artist artist : artistPaging.getItems()) {
            String imageUrl = artist.getImages().length > 0 ? artist.getImages()[0].getUrl() : null;
            
            result.add(SpotifyStatsDTO.TopArtist.builder()
                    .id(artist.getId())
                    .name(artist.getName())
                    .imageUrl(imageUrl)
                    .popularity(artist.getPopularity())
                    .genres(Arrays.asList(artist.getGenres()))
                    .rank(rank++)
                    .build());
        }
        
        return result;
    }
    
    private List<SpotifyStatsDTO.TopTrack> fetchTopTracks(SpotifyApi spotifyApi, String timeRange) 
            throws IOException, SpotifyWebApiException, ParseException {
        
        GetUsersTopTracksRequest request = spotifyApi.getUsersTopTracks()
                .time_range(timeRange)
                .limit(20)
                .build();
        
        Paging<Track> trackPaging = request.execute();
        List<SpotifyStatsDTO.TopTrack> result = new ArrayList<>();
        
        int rank = 1;
        for (Track track : trackPaging.getItems()) {
            String artistName = track.getArtists().length > 0 ? track.getArtists()[0].getName() : "Unknown";
            String albumImageUrl = track.getAlbum().getImages().length > 0 
                    ? track.getAlbum().getImages()[0].getUrl() : null;
            
            result.add(SpotifyStatsDTO.TopTrack.builder()
                    .id(track.getId())
                    .name(track.getName())
                    .artistName(artistName)
                    .albumName(track.getAlbum().getName())
                    .albumImageUrl(albumImageUrl)
                    .durationMs(track.getDurationMs())
                    .popularity(track.getPopularity())
                    .rank(rank++)
                    .build());
        }
        
        return result;
    }
    
    private List<SpotifyStatsDTO.GenreStat> calculateGenreStats(List<SpotifyStatsDTO.TopArtist> topArtists) {
        Map<String, Integer> genreCounts = new HashMap<>();
        int totalGenres = 0;
        
        for (SpotifyStatsDTO.TopArtist artist : topArtists) {
            if (artist.getGenres() != null) {
                for (String genre : artist.getGenres()) {
                    genreCounts.merge(genre, 1, Integer::sum);
                    totalGenres++;
                }
            }
        }
        
        final int total = totalGenres;
        return genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> SpotifyStatsDTO.GenreStat.builder()
                        .genre(entry.getKey())
                        .count(entry.getValue())
                        .percentage(total > 0 ? (entry.getValue() * 100.0) / total : 0)
                        .build())
                .collect(Collectors.toList());
    }
    
    private SpotifyStatsDTO.ListeningStats fetchListeningStats(
            SpotifyApi spotifyApi, 
            List<SpotifyStatsDTO.TopArtist> topArtists,
            List<SpotifyStatsDTO.TopTrack> topTracks) {
        
        int totalPlaylists = 0;
        int totalSavedTracks = 0;
        int totalFollowedArtists = 0;
        
        try {
            // Get playlists count
            GetListOfCurrentUsersPlaylistsRequest playlistsRequest = spotifyApi.getListOfCurrentUsersPlaylists()
                    .limit(1)
                    .build();
            totalPlaylists = playlistsRequest.execute().getTotal();
            
            // Get saved tracks count (using playlists as proxy)
            totalSavedTracks = totalPlaylists * 50; // Estimate based on average playlist size
            
            // Get followed artists count
            GetUsersFollowedArtistsRequest followedArtistsRequest = spotifyApi.getUsersFollowedArtists(
                    se.michaelthelin.spotify.enums.ModelObjectType.ARTIST)
                    .limit(1)
                    .build();
            var artistResult = followedArtistsRequest.execute();
            totalFollowedArtists = artistResult != null ? 
                    artistResult.getItems().length : 0;
            
        } catch (Exception e) {
            log.warn("Error fetching some listening stats: {}", e.getMessage());
        }
        
        // Calculate average popularity
        int avgTrackPopularity = topTracks.isEmpty() ? 0 : 
                (int) topTracks.stream().mapToInt(SpotifyStatsDTO.TopTrack::getPopularity).average().orElse(0);
        int avgArtistPopularity = topArtists.isEmpty() ? 0 : 
                (int) topArtists.stream().mapToInt(SpotifyStatsDTO.TopArtist::getPopularity).average().orElse(0);
        
        return SpotifyStatsDTO.ListeningStats.builder()
                .totalPlaylists(totalPlaylists)
                .totalSavedTracks(totalSavedTracks)
                .totalFollowedArtists(totalFollowedArtists)
                .averageTrackPopularity(avgTrackPopularity)
                .averageArtistPopularity(avgArtistPopularity)
                .build();
    }
}
