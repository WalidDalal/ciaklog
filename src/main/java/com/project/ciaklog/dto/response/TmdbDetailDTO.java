// TmdbDetailDTO.java
package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.MediaType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TmdbDetailDTO {
    private Long tmdbId;
    private String title;
    private MediaType mediaType;
    private String posterPath;
    private Integer releaseYear;
    private String overview;
    private List<String> genres;
    private List<String> cast; // top 5 attori
    private Double votoTmdb;
    private Double votoMedioCiakLog;
    private Integer numeroVotiCiakLog;
}