package com.project.guardrail.scheduler;

import com.project.guardrail.repository.NotificationRepository;
import com.project.guardrail.repository.UserRepository;
import com.project.guardrail.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000)
    public void processPendingNotifications() {

        userRepository.findAll().forEach(user -> {
            notificationService.processNotifications(user.getId());
        });

    }

}
