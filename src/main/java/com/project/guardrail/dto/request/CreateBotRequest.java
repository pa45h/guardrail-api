package com.project.guardrail.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBotRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String personaDescription;
}

