package com.soma.server.service;

import com.soma.server.entity.User;
import com.soma.server.entity.SpotifyUserDetails;
import com.soma.server.repository.SpotifyUserDetailsRepository;
import com.soma.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.michaelthelin.spotify.model_objects.specification.Image;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpotifyUserDetailsRepository spotifyUserDetailsRepository;

    @InjectMocks
    private UserService userService;

    private se.michaelthelin.spotify.model_objects.specification.User spotifyUser;
    private User appUser;
    private SpotifyUserDetails spotifyDetails;

    @BeforeEach
    void setUp() {
        // Mock Spotify User object manually since it has a private builder
        // Using mock() for external library final classes if needed, or just assuming structure
        spotifyUser = mock(se.michaelthelin.spotify.model_objects.specification.User.class);
        lenient().when(spotifyUser.getId()).thenReturn("spotify_123");
        lenient().when(spotifyUser.getDisplayName()).thenReturn("Test User");
        lenient().when(spotifyUser.getEmail()).thenReturn("test@example.com");
        lenient().when(spotifyUser.getImages()).thenReturn(new Image[]{});

        appUser = new User();
        appUser.setId(1L);
        appUser.setUsername("spotify_123");
        appUser.setEmail("test@example.com");

        spotifyDetails = new SpotifyUserDetails();
        spotifyDetails.setSpotifyUserId("spotify_123");
        spotifyDetails.setUser(appUser);
    }

    @Test
    @DisplayName("saveOrUpdateUser should update existing user if Spotify ID matches")
    void testSaveOrUpdateUser_ExistingSpotifyId() {
        // Given
        when(spotifyUserDetailsRepository.findBySpotifyUserId("spotify_123"))
                .thenReturn(Optional.of(spotifyDetails));
        when(userRepository.save(any(User.class))).thenReturn(appUser);

        // When
        User result = userService.saveOrUpdateUser(spotifyUser, "access_token", "refresh_token");

        // Then
        assertNotNull(result);
        assertEquals(appUser.getId(), result.getId());
        verify(userRepository).save(appUser);
        verify(spotifyUserDetailsRepository, times(2)).findBySpotifyUserId("spotify_123");
    }

    @Test
    @DisplayName("saveOrUpdateUser should create new user if not found")
    void testSaveOrUpdateUser_NewUser() {
        // Given
        when(spotifyUserDetailsRepository.findBySpotifyUserId("spotify_123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(2L);
            return u;
        });

        // When
        User result = userService.saveOrUpdateUser(spotifyUser, "access_token", "refresh_token");

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("spotify_spotify_123", result.getUsername()); // Logic prefixes "spotify_"
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("getSpotifyUserDetails should return details when found")
    void testGetSpotifyUserDetails() {
        // Given
        when(spotifyUserDetailsRepository.findBySpotifyUserId("spotify_123"))
                .thenReturn(Optional.of(spotifyDetails));

        // When
        Optional<SpotifyUserDetails> result = userService.getSpotifyUserDetails("spotify_123");

        // Then
        assertTrue(result.isPresent());
        assertEquals("spotify_123", result.get().getSpotifyUserId());
    }
}

