package com.soma.server.repository;

import com.soma.server.entity.SpotifyUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpotifyUserDetailsRepository extends JpaRepository<SpotifyUserDetails, Long> {
    Optional<SpotifyUserDetails> findBySpotifyUserId(String spotifyUserId);
    
    @Query("SELECT s FROM SpotifyUserDetails s LEFT JOIN FETCH s.user WHERE s.spotifyUserId = :spotifyUserId")
    Optional<SpotifyUserDetails> findBySpotifyUserIdWithUser(@Param("spotifyUserId") String spotifyUserId);
    
    @Query("SELECT s FROM SpotifyUserDetails s WHERE s.user.id = :userId")
    Optional<SpotifyUserDetails> findByUserId(@Param("userId") Long userId);
}
