// ChartUserDTO.java
package com.project.ciaklog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChartUserDTO {
    private String username;
    private Integer score;
    private Integer numeroRecensioni;
}