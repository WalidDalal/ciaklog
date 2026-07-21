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
    private List<CastMember> cast; // top 5 attori con foto
    private Double tmdbRating;
    private Double ciakLogAverageRating;
    private Integer ciakLogVoteCount;
    // Numero totale di stagioni per le serie
    // TV — null per i film. Usato per non far scegliere una stagione oltre
    // quelle che esistono davvero
    private Integer numberOfSeasons;

    @Getter
    @Builder
    public static class CastMember {
        private String name;
        private String photoPath; // profile_path da TMDB, può essere null
    }
}