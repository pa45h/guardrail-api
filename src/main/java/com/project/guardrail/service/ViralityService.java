package com.project.guardrail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViralityService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int BOT_REPLY_SCORE = 1;
    private static final int HUMAN_LIKE_SCORE = 20;
    private static final int HUMAN_COMMENT_SCORE = 50;

    public Long addBotReplyScore(Long postId) {
        return incrementScore(postId, BOT_REPLY_SCORE);
    }

    public Long addHumanLikeScore(Long postId) {
        return incrementScore(postId, HUMAN_LIKE_SCORE);
    }

    public Long addHumanCommentScore(Long postId) {
        return incrementScore(postId, HUMAN_COMMENT_SCORE);
    }

    public Long getViralityScore(Long postId) {

        String key = buildViralityKey(postId);

        Object score =
                redisTemplate.opsForValue().get(key);

        if (score == null) {
            return 0L;
        }

        if (score instanceof Long) {
            return (Long) score;
        }

        if (score instanceof Integer) {
            return ((Integer) score).longValue();
        }

        if (score instanceof String) {
            return Long.valueOf((String) score);
        }

        throw new RuntimeException(
                "Unsupported Redis score type: "
                        + score.getClass()
        );
    }

    private Long incrementScore(Long postId, int incrementValue) {
        String key = buildViralityKey(postId);
        return redisTemplate.opsForValue().increment(key, incrementValue);
    }

    private String buildViralityKey(Long postId) {
        return "post:" + postId + ":virality_score";
    }
}
