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

        System.out.println("QUEUE NOTIFICATION CALLED");

        String key = buildNotificationQueueKey(userId);

        System.out.println("KEY: " + key);

        redisTemplate.opsForSet()
                .add("pending_notification_users", userId);

        redisTemplate.opsForList()
                .rightPush(key, message);

        Long listSize =
                redisTemplate.opsForList().size(key);

        System.out.println("LIST SIZE: " + listSize);

        Boolean exists =
                redisTemplate.hasKey(key);

        System.out.println("KEY EXISTS: " + exists);
    }

    public void handleBotNotification(Long userId, String message) {
        String cooldownKey = "user:" + userId + ":notif_cooldown";

        Boolean sentNow = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "ACTIVE", java.time.Duration.ofMinutes(15));

        if (Boolean.TRUE.equals(sentNow)) {
            System.out.println("Push Notification Sent to User: " + message);
        } else {
            queueNotification(userId, message);
        }
    }

    public void processNotifications(Long userId) {
        String key = buildNotificationQueueKey(userId);

        Long size = redisTemplate.opsForList().size(key);

        if (size == null || size == 0) {
            return;
        }

        List<Object> notifications = redisTemplate.opsForList().range(key, 0, -1);

        String summarizedMessage = buildSummaryMessage(notifications);

        System.out.println("Summarized Push Notification: " + summarizedMessage);

        Notification notification = Notification.builder()
                .recipientUserId(userId)
                .message(summarizedMessage)
                .processed(true)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        redisTemplate.delete(key);
    }

    private String buildSummaryMessage(List<Object> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return "You have new notifications.";
        }

        int totalNotifications = notifications.size();

        if (totalNotifications == 1) {
            return notifications.get(0).toString();
        }

        String first = notifications.get(0).toString();
        return first + " and [" + (totalNotifications - 1) + "] others interacted with your posts.";
    }

    private String buildNotificationQueueKey(Long userId) {
        return "user:" + userId + ":pending_notifications";
    }

}
