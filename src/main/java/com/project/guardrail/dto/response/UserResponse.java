package com.project.guardrail.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;

    private String username;

    private boolean isPremium;

    private LocalDateTime createdAt;
}

