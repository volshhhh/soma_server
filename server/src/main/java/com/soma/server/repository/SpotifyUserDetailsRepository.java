package com.soma.server.repository;

import com.soma.server.entity.SpotifyUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotifyUserDetailsRepository extends JpaRepository<SpotifyUserDetails, Long> {
    Optional<SpotifyUserDetails> findBySpotifyUserId(String spotifyUserId);
}
