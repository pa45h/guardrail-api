package com.project.guardrail.dto.request;

import com.project.guardrail.entity.enums.AuthorType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLikeRequest {

    @NotNull
    private Long authorId;

    @NotNull
    private AuthorType authorType;

}

