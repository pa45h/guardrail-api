package com.project.guardrail.dto.request;

import com.project.guardrail.entity.enums.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotNull
    private Long authorId;

    @NotNull
    private AuthorType authorType;

    @NotBlank
    private String content;

    private Long parentCommentId;
}