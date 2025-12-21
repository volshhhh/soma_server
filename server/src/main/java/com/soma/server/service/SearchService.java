package com.soma.server.service;

import com.soma.server.config.SpotifyConfig;
import com.soma.server.dto.SearchResultDTO;
import com.soma.server.dto.SearchResultDTO.*;
import com.soma.server.entity.SpotifyUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching Spotify catalog.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SpotifyConfig spotifyConfig;
    private final UserService userService;

    /**
     * Search Spotify catalog for tracks, artists, albums, or playlists.
     */
    public Optional<SearchResultDTO> search(String query, String userId, String type, int limit, int offset) {
        try {
            Optional<SpotifyUserDetails> userDetailsOpt = userService.getSpotifyUserDetails(userId);
            if (userDetailsOpt.isEmpty()) {
                log.warn("User not found for search: {}", userId);
                return Optional.empty();
            }

            SpotifyUserDetails userDetails = userDetailsOpt.get();
            SpotifyApi spotifyApi = spotifyConfig.getSpotifyObject();
            spotifyApi.setAccessToken(userDetails.getAccessToken());

            SearchResultDTO.SearchResultDTOBuilder resultBuilder = SearchResultDTO.builder()
                    .query(query);

            int totalResults = 0;

            // Determine which types to search
            Set<String> searchTypes = parseSearchTypes(type);

            // Search tracks
            if (searchTypes.contains("track")) {
                var request = spotifyApi.searchTracks(query)
                        .limit(limit)
                        .offset(offset)
                        .build();
                
                var trackPaging = request.execute();
                List<TrackItem> tracks = Arrays.stream(trackPaging.getItems())
                        .map(this::mapTrack)
                        .collect(Collectors.toList());
                
                resultBuilder.tracks(tracks);
                totalResults += trackPaging.getTotal();
            }

            // Search artists
            if (searchTypes.contains("artist")) {
                var request = spotifyApi.searchArtists(query)
                        .limit(limit)
                        .offset(offset)
                        .build();
                
                var artistPaging = request.execute();
                List<ArtistItem> artists = Arrays.stream(artistPaging.getItems())
                        .map(this::mapArtist)
                        .collect(Collectors.toList());
                
                resultBuilder.artists(artists);
                totalResults += artistPaging.getTotal();
            }

            // Search albums
            if (searchTypes.contains("album")) {
                var request = spotifyApi.searchAlbums(query)
                        .limit(limit)
                        .offset(offset)
                        .build();
                
                var albumPaging = request.execute();
                List<AlbumItem> albums = Arrays.stream(albumPaging.getItems())
                        .map(this::mapAlbum)
                        .collect(Collectors.toList());
                
                resultBuilder.albums(albums);
                totalResults += albumPaging.getTotal();
            }

            // Search playlists
            if (searchTypes.contains("playlist")) {
                var request = spotifyApi.searchPlaylists(query)
                        .limit(limit)
                        .offset(offset)
                        .build();
                
                var playlistPaging = request.execute();
                List<PlaylistItem> playlists = Arrays.stream(playlistPaging.getItems())
                        .map(this::mapPlaylist)
                        .collect(Collectors.toList());
                
                resultBuilder.playlists(playlists);
                totalResults += playlistPaging.getTotal();
            }

            resultBuilder.totalResults(totalResults);
            
            log.info("Search completed for query '{}': {} results", query, totalResults);
            return Optional.of(resultBuilder.build());

        } catch (Exception e) {
            log.error("Search failed for query '{}': {}", query, e.getMessage());
            return Optional.empty();
        }
    }

    private Set<String> parseSearchTypes(String type) {
        if (type == null || type.equalsIgnoreCase("all")) {
            return Set.of("track", "artist", "album", "playlist");
        }
        return Arrays.stream(type.toLowerCase().split(","))
                .map(String::trim)
                .filter(t -> Set.of("track", "artist", "album", "playlist").contains(t))
                .collect(Collectors.toSet());
    }

    private TrackItem mapTrack(Track track) {
        return TrackItem.builder()
                .id(track.getId())
                .name(track.getName())
                .artists(Arrays.stream(track.getArtists())
                        .map(ArtistSimplified::getName)
                        .collect(Collectors.toList()))
                .album(track.getAlbum() != null ? track.getAlbum().getName() : null)
                .imageUrl(getFirstImageUrl(track.getAlbum() != null ? track.getAlbum().getImages() : null))
                .durationMs(track.getDurationMs())
                .previewUrl(track.getPreviewUrl())
                .spotifyUrl(track.getExternalUrls() != null ? track.getExternalUrls().get("spotify") : null)
                .explicit(track.getIsExplicit())
                .build();
    }

    private ArtistItem mapArtist(Artist artist) {
        return ArtistItem.builder()
                .id(artist.getId())
                .name(artist.getName())
                .imageUrl(getFirstImageUrl(artist.getImages()))
                .genres(artist.getGenres() != null ? Arrays.asList(artist.getGenres()) : List.of())
                .followers(artist.getFollowers() != null ? artist.getFollowers().getTotal() : 0)
                .popularity(artist.getPopularity())
                .spotifyUrl(artist.getExternalUrls() != null ? artist.getExternalUrls().get("spotify") : null)
                .build();
    }

    private AlbumItem mapAlbum(AlbumSimplified album) {
        return AlbumItem.builder()
                .id(album.getId())
                .name(album.getName())
                .artists(Arrays.stream(album.getArtists())
                        .map(ArtistSimplified::getName)
                        .collect(Collectors.toList()))
                .imageUrl(getFirstImageUrl(album.getImages()))
                .releaseDate(album.getReleaseDate())
                .totalTracks(0) // Not available in AlbumSimplified
                .albumType(album.getAlbumType() != null ? album.getAlbumType().toString() : null)
                .spotifyUrl(album.getExternalUrls() != null ? album.getExternalUrls().get("spotify") : null)
                .build();
    }

    private PlaylistItem mapPlaylist(PlaylistSimplified playlist) {
        return PlaylistItem.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(null) // Not available in PlaylistSimplified
                .owner(playlist.getOwner() != null ? playlist.getOwner().getDisplayName() : null)
                .imageUrl(getFirstImageUrl(playlist.getImages()))
                .totalTracks(playlist.getTracks() != null ? playlist.getTracks().getTotal() : 0)
                .isPublic(playlist.getIsPublicAccess() != null && playlist.getIsPublicAccess())
                .spotifyUrl(playlist.getExternalUrls() != null ? playlist.getExternalUrls().get("spotify") : null)
                .build();
    }

    private String getFirstImageUrl(Image[] images) {
        if (images == null || images.length == 0) {
            return null;
        }
        return images[0].getUrl();
    }
}
