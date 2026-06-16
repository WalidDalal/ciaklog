// ChartItemDTO.java
package com.project.ciaklog.dto.response;

import com.project.ciaklog.entity.MediaType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartItemDTO {
    private Long tmdbId;
    private String title;
    private MediaType mediaType;
    private String posterPath;
    private Double votoMedio;
    private Integer numeroVoti;
}