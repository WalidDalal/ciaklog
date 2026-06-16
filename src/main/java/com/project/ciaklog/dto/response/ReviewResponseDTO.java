// ReviewResponseDTO.java
package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.MediaType;
import com.project.ciaklog.entity.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReviewResponseDTO {
    private UUID id;
    private String username;
    private Long tmdbId;
    private MediaType mediaType;
    private Integer rating;
    private String text;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}