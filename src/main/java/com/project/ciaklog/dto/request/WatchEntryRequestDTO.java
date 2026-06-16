// WatchEntryRequestDTO.java
package com.project.ciaklog.dto.request;

import com.project.ciaklog.entity.MediaType;
import com.project.ciaklog.entity.WatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchEntryRequestDTO {

    @NotNull(message = "tmdbId obbligatorio")
    private Long tmdbId;

    @NotNull(message = "mediaType obbligatorio")
    private MediaType mediaType;

    @NotBlank(message = "title obbligatorio")
    private String title;

    private String posterPath;
    private Integer releaseYear;
    private String genres; // CSV già risolto lato frontend da TMDB

    @NotNull(message = "status obbligatorio")
    private WatchStatus status;

    private Integer currentSeason; // nullable, solo per TV + WATCHING
}