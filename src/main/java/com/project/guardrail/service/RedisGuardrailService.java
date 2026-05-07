package com.project.guardrail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisGuardrailService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_BOT_REPLIES = 100;

    public boolean incrementBotReplyCount(Long postId) {
        String key = "post:" + postId + ":bot_count";

        Long count = redisTemplate.opsForValue().increment(key, 1);

        return count != null && count <= MAX_BOT_REPLIES;
    }

    public boolean checkCooldown(Long botId, Long humanId) {

        String key =
                "cooldown:bot_" + botId + ":human_" + humanId;

        Boolean success =
                redisTemplate.opsForValue()
                        .setIfAbsent(
                                key,
                                "ACTIVE",
                                Duration.ofMinutes(10)
                        );

        return Boolean.TRUE.equals(success);
    }
}
