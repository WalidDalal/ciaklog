package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ContentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartItemResponse {
    private Long tmdbId;
    private String title;
    private ContentType contentType;
    private String posterPath;
    private Double averageRating;
    private Integer totalVotes;
}