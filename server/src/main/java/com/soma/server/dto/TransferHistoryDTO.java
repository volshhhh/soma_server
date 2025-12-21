package com.soma.server.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TransferHistoryDTO {
    private Long id;
    private String yandexPlaylistLink;
    private String spotifyPlaylistLink;
    private String status;
    private Integer trackCount;
    private Integer transferredCount;
    private LocalDateTime createdAt;
    private String errorMessage;
}
