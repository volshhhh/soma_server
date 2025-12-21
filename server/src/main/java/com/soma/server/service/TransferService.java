package com.soma.server.service;

import com.soma.server.entity.PlaylistTransfer;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.parser.YmParser;
import com.soma.server.repository.PlaylistTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.special.SnapshotResult;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.CreatePlaylistRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final PlaylistTransferRepository playlistTransferRepository;
    private final YmParser ymParser;

    public Long createTransferRecord(SpotifyUserDetails userDetails, String yandexLink, String spotifyLink) {
        PlaylistTransfer transfer = new PlaylistTransfer();
        transfer.setUser(userDetails.getUser());
        transfer.setYandexPlaylistLink(yandexLink);
        transfer.setSpotifyPlaylistLink(spotifyLink);
        transfer.setStatus(PlaylistTransfer.TransferStatus.PENDING);
        transfer.setTrackCount(0);
        transfer.setTransferredCount(0);
        
        return playlistTransferRepository.save(transfer).getId();
    }

    @Async
    public void processTransfer(Long transferId, String accessToken, String playlistName, boolean isNewPlaylist, String existingPlaylistId) {
        log.info("Starting async transfer for ID: {}", transferId);
        PlaylistTransfer transfer = playlistTransferRepository.findById(transferId).orElse(null);
        if (transfer == null) return;

        transfer.setStatus(PlaylistTransfer.TransferStatus.IN_PROGRESS);
        playlistTransferRepository.save(transfer);

        try {
            // 1. Parse Yandex
            Map<String, List<String>> songs = ymParser.parsing(transfer.getYandexPlaylistLink());

            if (songs.isEmpty()) {
                transfer.setStatus(PlaylistTransfer.TransferStatus.FAILED);
                transfer.setErrorMessage("No songs found in Yandex playlist");
                playlistTransferRepository.save(transfer);
                return;
            }

            // 2. Search Tracks
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                    .setAccessToken(accessToken)
                    .build();

            List<String> urisList = getTracksURI(songs, spotifyApi);
            String[] uris = urisList.toArray(new String[0]);
            
            transfer.setTrackCount(uris.length);
            playlistTransferRepository.save(transfer);

            if (uris.length == 0) {
                transfer.setStatus(PlaylistTransfer.TransferStatus.FAILED);
                transfer.setErrorMessage("No matching tracks found on Spotify");
                playlistTransferRepository.save(transfer);
                return;
            }

            // 3. Create or Get Playlist
            String playlistId = existingPlaylistId;
            if (isNewPlaylist) {
                try {
                    String userId = spotifyApi.getCurrentUsersProfile().build().execute().getId();
                    CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId, playlistName).build();
                    final Playlist playlist = createPlaylistRequest.execute();
                    playlistId = playlist.getId();
                    transfer.setSpotifyPlaylistLink(playlist.getExternalUrls().get("spotify"));
                    playlistTransferRepository.save(transfer);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create playlist: " + e.getMessage());
                }
            }

            // 4. Add Items in Batches (100 max)
            int batchSize = 100;
            int total = uris.length;
            
            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(total, i + batchSize);
                String[] batch = Arrays.copyOfRange(uris, i, end);
                
                addingItemsToPlayList(batch, spotifyApi, playlistId);
                
                transfer.setTransferredCount(end);
                playlistTransferRepository.save(transfer);
                
                // Small delay to respect rate limits if needed
                Thread.sleep(200);
            }

            transfer.setStatus(PlaylistTransfer.TransferStatus.COMPLETED);
            playlistTransferRepository.save(transfer);
            log.info("Transfer {} completed successfully", transferId);

        } catch (Exception e) {
            log.error("Error in transfer {}: {}", transferId, e.getMessage(), e);
            transfer.setStatus(PlaylistTransfer.TransferStatus.FAILED);
            transfer.setErrorMessage(e.getMessage());
            playlistTransferRepository.save(transfer);
        }
    }

    private List<String> getTracksURI(Map<String, List<String>> songs, SpotifyApi spotifyApi) {
        List<String> uris = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : songs.entrySet()) {
            String artist = entry.getKey();
            List<String> songList = entry.getValue();

            for (String song : songList) {
                String q = artist + " " + song;
                try {
                    SearchTracksRequest searchTracksRequest = spotifyApi.searchTracks(q)
                            .limit(1)
                            .build();
                    final Paging<Track> trackPaging = searchTracksRequest.execute();
                    if (trackPaging.getItems().length > 0) {
                        uris.add(trackPaging.getItems()[0].getUri());
                    }
                } catch (Exception e) {
                    log.error("Error searching for track {}: {}", q, e.getMessage());
                }
            }
        }
        return uris;
    }

    private void addingItemsToPlayList(String[] uris, SpotifyApi spotifyApi, String playlistId) throws IOException, SpotifyWebApiException, ParseException {
        AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi
                .addItemsToPlaylist(playlistId, uris)
                .build();
        SnapshotResult snapshotResult = addItemsToPlaylistRequest.execute();
        log.debug("Added batch items. Snapshot: {}", snapshotResult.getSnapshotId());
    }
}
