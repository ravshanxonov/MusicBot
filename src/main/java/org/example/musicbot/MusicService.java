package org.example.musicbot;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendAudio;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class MusicService {

    private final String YOUTUBE_API_KEY = "AIzaSyDcMdEJzFhLC2wQ8VU1X4J8p9yXNVln-Hc";
    private final TelegramBot telegramBot;
    private final YouTube youtubeService;

    public MusicService(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
        this.youtubeService = new YouTube.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                new com.google.api.client.json.jackson2.JacksonFactory(),
                request -> {}
        ).setApplicationName("youtube-telegram-bot").build();
    }




    public void search(String text, Long chatId) {
        try {
            YouTube.Search.List searchRequest = youtubeService.search().list("id,snippet");
            searchRequest.setQ(text);
            searchRequest.setType("video");
            searchRequest.setMaxResults(10L);
            searchRequest.setKey(YOUTUBE_API_KEY);

            SearchListResponse searchResponse = searchRequest.execute();
            List<SearchResult> searchResults = searchResponse.getItems();

            StringBuilder messageBuilder = new StringBuilder();
            InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

            int index = 1;
            for (SearchResult result : searchResults) {
                String videoId = result.getId().getVideoId();
                String title = result.getSnippet().getTitle();
                String isoDuration = getVideoDuration(videoId);
                String formattedDuration = parseDuration(isoDuration);

                messageBuilder.append(index).append(". ").append(title).append(" ").append(formattedDuration).append("\n");

                index++;
            }

            InlineKeyboardButton[] firstRowButtons = new InlineKeyboardButton[5];
            InlineKeyboardButton[] secondRowButtons = new InlineKeyboardButton[5];

            for (int i = 0; i < 5; i++) {
                firstRowButtons[i] = new InlineKeyboardButton(String.valueOf(i + 1))
                        .callbackData("music_" + searchResults.get(i).getId().getVideoId());
                secondRowButtons[i] = new InlineKeyboardButton(String.valueOf(i + 6))
                        .callbackData("music_" + searchResults.get(i + 5).getId().getVideoId());
            }

            inlineKeyboard.addRow(firstRowButtons);
            inlineKeyboard.addRow(secondRowButtons);

            inlineKeyboard.addRow(
                    new InlineKeyboardButton("⬅️").callbackData("back_" + text),
                    new InlineKeyboardButton("❌").callbackData("cancel"),
                    new InlineKeyboardButton("➡️").callbackData("next_" + text)
            );

            telegramBot.execute(new SendMessage(chatId, messageBuilder.toString()).replyMarkup(inlineKeyboard));

        } catch (IOException e) {
            e.printStackTrace();
            telegramBot.execute(new SendMessage(chatId, "Xatolik yuz berdi. Iltimos, qaytadan urinib ko'ring."));
        }
    }



    private String parseDuration(String isoDuration) {
        int hours = 0, minutes = 0, seconds = 0;

        String time = isoDuration.substring(2); // "PT" ni olib tashlaymiz
        String[] timeParts = time.split("H|M|S");

        if (time.contains("H")) {
            hours = Integer.parseInt(timeParts[0]);
            time = time.substring(time.indexOf("H") + 1);
        }
        if (time.contains("M")) {
            minutes = Integer.parseInt(timeParts[hours > 0 ? 1 : 0]);
            time = time.substring(time.indexOf("M") + 1);
        }
        if (time.contains("S")) {
            seconds = Integer.parseInt(timeParts[hours > 0 && minutes > 0 ? 2 : hours > 0 || minutes > 0 ? 1 : 0]);
        }

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }



    private String getVideoDuration(String videoId) throws IOException {
        YouTube.Videos.List videoRequest = youtubeService.videos().list("contentDetails");
        videoRequest.setId(videoId);
        videoRequest.setKey(YOUTUBE_API_KEY);

        VideoListResponse videoResponse = videoRequest.execute();
        List<Video> videos = videoResponse.getItems();

        if (!videos.isEmpty()) {
            return videos.get(0).getContentDetails().getDuration();
        }
        return "Noma'lum";
    }

    @SneakyThrows
    public void download(String musicQuery, Long chatId) {
        String videoUrl = "https://www.ssyoutube.com/watch?v=" + musicQuery;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://youtube-mp3-downloader2.p.rapidapi.com/ytmp3/ytmp3/?url="+videoUrl))
                .header("x-rapidapi-key", "0e0e94279dmsh6708a24f3ab08a5p1736d8jsn8a71c057604e")
                .header("x-rapidapi-host", "youtube-mp3-downloader2.p.rapidapi.com")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonObject = new JSONObject(response.body());

        String dlink = jsonObject.getString("dlink");
        byte[] fileData = downloadFileAsBytes(dlink);
            if (fileData != null) {
                telegramBot.execute(new SendAudio(chatId, fileData));
            }
        };

    byte[] downloadFileAsBytes(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }
                return baos.toByteArray();
            }
        } catch (Exception e) {
            System.err.println("Faylni yuklashda xatolik: " + e.getMessage());
            return null;
        }
    }
}
