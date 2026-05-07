package com.project.guardrail.controller;

import com.project.guardrail.dto.request.CreateBotRequest;
import com.project.guardrail.dto.response.BotResponse;
import com.project.guardrail.service.BotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @PostMapping
    public ResponseEntity<BotResponse> createBot(@Valid @RequestBody CreateBotRequest request) {
        BotResponse response = botService.createBot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{botId}")
    public ResponseEntity<BotResponse> getBot(@PathVariable Long botId) {
        BotResponse response = botService.getBot(botId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BotResponse>> getAllBots() {
        List<BotResponse> responses = botService.getAllBots();
        return ResponseEntity.ok(responses);
    }
}

