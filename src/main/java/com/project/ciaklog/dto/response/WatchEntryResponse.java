// WatchEntryResponseDTO.java
package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.WatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class WatchEntryResponse {
    private UUID id;
    private Long tmdbId;
    private ContentType contentType;
    private String title;
    private String posterPath;
    private Integer releaseYear;
    private WatchStatus status;
    private Integer currentSeason;
    private LocalDate watchedDate;
    private LocalDateTime lastStatusUpdate;
    // nessun campo rating — il voto si recupera da Review
}