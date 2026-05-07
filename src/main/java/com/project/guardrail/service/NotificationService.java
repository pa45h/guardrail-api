package com.project.guardrail.service;

import com.project.guardrail.entity.Notification;
import com.project.guardrail.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final NotificationRepository notificationRepository;

    public void queueNotification(Long userId, String message) {
        String key = buildNotificationQueueKey(userId);
        redisTemplate.opsForList().rightPush(key, message);
    }

    public void processNotifications(Long userId) {
        String key = buildNotificationQueueKey(userId);

        Long size = redisTemplate.opsForList().size(key);

        if (size == null || size == 0) {
            return;
        }

        List<Object> notifications = redisTemplate.opsForList().range(key, 0, -1);

        String summarizedMessage = buildSummaryMessage(notifications);

        Notification notification = Notification.builder()
                .recipientUserId(userId)
                .message(summarizedMessage)
                .processed(true)
                .createdAt(LocalDateTime.now())
                .build();
        ;

        notificationRepository.save(notification);

        redisTemplate.delete(key);
    }

    private String buildSummaryMessage(List<Object> notifications) {
        int totalNotifications = notifications.size();

        return "You have " + totalNotifications + " new notifications.";
    }

    private String buildNotificationQueueKey(Long userId) {
        return "user:" + userId + ":pending_notifications";
    }

}
