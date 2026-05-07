package com.project.guardrail.scheduler;

import com.project.guardrail.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000)  // 5 minutes
    public void processPendingNotifications() {

        Set<Object> users =
                redisTemplate.opsForSet()
                        .members("pending_notification_users");

        if (users == null || users.isEmpty()) {
            return;
        }

        for (Object userIdObj : users) {

            Long userId =
                    Long.parseLong(userIdObj.toString());

            notificationService.processNotifications(userId);

            redisTemplate.opsForSet()
                    .remove(
                            "pending_notification_users",
                            userId
                    );
        }
    }
}
