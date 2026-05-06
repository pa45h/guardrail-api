package com.project.guardrail.dto.response;

import com.project.guardrail.entity.enums.AuthorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {

    private Long id;

    private Long postId;

    private Long authorId;

    private AuthorType authorType;

    private String content;

    private Integer depthLevel;

    private Long parentCommentId;

    private LocalDateTime createdAt;
}