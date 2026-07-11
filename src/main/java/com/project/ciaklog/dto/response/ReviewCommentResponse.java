package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.ReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReviewCommentResponse {
    private UUID id;
    private UUID reviewId;
    private String authorUsername;
    private String text;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
