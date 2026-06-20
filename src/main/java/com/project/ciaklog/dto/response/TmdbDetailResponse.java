package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TmdbDetailResponse {
    private Long tmdbId;
    private String title;
    private ContentType contentType;
    private String posterPath;
    private Integer releaseYear;
    private String overview;
    private List<String> genres;
    private List<String> cast; // top 5 attori
    private Double tmdbRating;
    private Double ciakLogAverageRating;
    private Integer ciakLogVoteCount;
}