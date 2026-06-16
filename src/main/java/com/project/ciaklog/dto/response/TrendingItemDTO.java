// TrendingItemDTO.java
package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.MediaType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrendingItemDTO {
    private Long tmdbId;
    private String title;
    private MediaType mediaType;
    private String posterPath;
    private Integer numeroRecensioniSettimana;
    private Double votoMedioCiakLog;
}