package org.example.musicbot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.example.musicbot.entity.User;
import org.example.musicbot.repo.UserRepo;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MusicBot {

    private final UserRepo userRepo;
    private final TelegramBot telegramBot;
    private final MusicService musicService;

    public void onUpdateReceived(Update update) {
        if (update.message() != null && update.message().text() != null) {
            Message message = update.message();
            String text = message.text();
            String chatType = message.chat().type().toString();
            int messageId = message.messageId();
            Long chatId = message.chat().id();

            if (isGroupChat(chatType)) {
                processGroupMessage(text, chatId, messageId);
            } else if ("private".equalsIgnoreCase(chatType)) {
                processPrivateMessage(text, chatId, messageId);
            }
        } else if (update.callbackQuery() != null && update.callbackQuery().data() != null) {
            String callbackData = update.callbackQuery().data();
            Long chatId = update.callbackQuery().message().chat().id();
            if (callbackData.startsWith("music_")) {
                String musicQuery = callbackData.substring(6);
                musicService.download(musicQuery, chatId);
            } else if (callbackData.startsWith("next_")) {

            } else if (callbackData.startsWith("back_")) {

            } else if ("cancel".equals(callbackData)) {
                telegramBot.execute(new DeleteMessage(chatId, update.callbackQuery().message().messageId()));
            }
        }
    }

    private boolean isGroupChat(String chatType) {
        return "group".equalsIgnoreCase(chatType) || "supergroup".equalsIgnoreCase(chatType);
    }

    private void processGroupMessage(String text, Long chatId, int messageId) {
        if (text.startsWith("/music")) {
            String musicQuery = text.substring(7);
            musicService.search(musicQuery, chatId);
        }
    }

    private void processPrivateMessage(String text, Long chatId, int messageId) {
        if ("/start".equalsIgnoreCase(text)) {
            startMethod(chatId);
        } else {
            musicService.search(text, chatId);
        }
    }

    public void startMethod(Long chatId) {
        if (userRepo.findByChatId(chatId) == null) {
            User user = new User();
            user.setChatId(chatId);
            userRepo.save(user);
        }
        telegramBot.execute(new SendMessage(chatId,
                "Salom! Ushbu Music Bot orqali siz musiqalarni qidirishingiz va yuklab olishingiz mumkin"));
    }
}
