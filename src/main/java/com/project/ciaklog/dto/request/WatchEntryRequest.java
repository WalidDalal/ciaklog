package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.ContentType;
import com.project.ciaklog.entity.WatchStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchEntryRequest {

    @NotNull(message = "tmdbId obbligatorio")
    private Long tmdbId;

    @NotNull(message = "contentType obbligatorio")
    private ContentType contentType;

    // title, posterPath, releaseYear, genres rimossi:
    // vengono recuperati server-side da TMDB in WatchEntryServiceImpl.addToLibrary()

    @NotNull(message = "status obbligatorio")
    private WatchStatus status;

    private Integer currentSeason; // nullable, solo per TV + WATCHING
}