// UserProfileResponseDTO.java
package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserProfileResponseDTO {
    private String username;
    private List<String> topGenres; // top 3 generi calcolati
    private String watchingTitle;   // nullable — "Sta guardando: X"
    private Integer watchingSeason; // nullable — solo se TV
    private LocalDateTime lastStatusUpdate; // per logica 30gg frontend
}