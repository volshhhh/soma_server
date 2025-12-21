package com.soma.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "playlist_transfers")
@Data
@NoArgsConstructor
public class PlaylistTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "yandex_playlist_link", length = 1024)
    private String yandexPlaylistLink;

    @Column(name = "spotify_playlist_link", length = 1024)
    private String spotifyPlaylistLink;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    @Column(name = "track_count")
    private Integer trackCount = 0;
    
    @Column(name = "transferred_count")
    private Integer transferredCount = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    public enum TransferStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        PARTIAL
    }
}
