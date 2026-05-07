package com.project.guardrail.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BotResponse {

    private Long id;

    private String name;

    private String personaDescription;
}

