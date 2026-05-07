package com.project.guardrail.dto.response;

import com.project.guardrail.entity.enums.AuthorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PostResponse {

    private Long id;

    private Long authorId;

    private AuthorType authorType;

    private String content;

    private Long viralityScore;

    private LocalDateTime createdAt;
}