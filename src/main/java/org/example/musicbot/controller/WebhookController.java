package org.example.musicbot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.example.musicbot.MusicBot;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final Gson gson = new Gson();
    private final MusicBot musicBot;

    @PostMapping("/")
    public String onUpdateReceived(@RequestBody String requestBody) throws JsonProcessingException {
        Update update = gson.fromJson(requestBody, Update.class);
        musicBot.onUpdateReceived(update);
        return "Webhook received";
    }
}