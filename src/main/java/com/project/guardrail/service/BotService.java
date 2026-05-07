package com.project.guardrail.service;

import com.project.guardrail.dto.request.CreateBotRequest;
import com.project.guardrail.dto.response.BotResponse;
import com.project.guardrail.entity.Bot;
import com.project.guardrail.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BotService {

    private final BotRepository botRepository;

    @Transactional
    public BotResponse createBot(CreateBotRequest request) {
        Bot bot = Bot.builder()
                .name(request.getName())
                .personaDescription(request.getPersonaDescription())
                .build();

        Bot savedBot = botRepository.save(bot);
        return mapToResponse(savedBot);
    }

    public BotResponse getBot(Long botId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));
        return mapToResponse(bot);
    }

    public List<BotResponse> getAllBots() {
        return botRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private BotResponse mapToResponse(Bot bot) {
        return BotResponse.builder()
                .id(bot.getId())
                .name(bot.getName())
                .personaDescription(bot.getPersonaDescription())
                .build();
    }
}

