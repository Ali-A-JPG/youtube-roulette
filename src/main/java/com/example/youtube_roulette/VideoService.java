package com.example.youtube_roulette;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.time.format.DateTimeParseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;



@Service
public class VideoService {
    public Map<String, String> getRandomVideo(String keyword) {
        if (keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must provide a video title");
        }
        String apiKey = System.getenv("youtube_api_key");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("You must provide a api key");
        }

        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();

        String url = "https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet&type=video&maxResults=5"
                + "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }

        String Json = response.body();
        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root;
        root = mapper.readTree(Json);
        JsonNode items;

        items = root.get("items");

        if (items == null || !items.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }
        if (items.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid request");
        }

        List<String> videoIds = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode idNode = item.path("id").path("videoId");
            if (idNode.isTextual()) {
                videoIds.add(idNode.asText());
            }
        }

        if (videoIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "invalid id");
        }
        String detailsUrl = "https://www.googleapis.com/youtube/v3/videos"
                + "?part=contentDetails"

                + "&id=" + String.join(",", videoIds)

                + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest detailsRequest = HttpRequest.newBuilder()
                .uri(URI.create(detailsUrl))
                .GET()
                .build();

        HttpResponse<String> detailsResponse;
        try {
            detailsResponse = client.send(detailsRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (detailsResponse.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, " failed");
        }
        JsonNode detailsRoot = mapper.readTree(detailsResponse.body());
        JsonNode detailsItems = detailsRoot.get("items");
        if (detailsItems == null || !detailsItems.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }

        Set<String> eligibleIds = new HashSet<>();
        for (JsonNode detail : detailsItems) {
            JsonNode idNode = detail.path("id");
            JsonNode durationNode = detail.path("contentDetails").path("duration");
            if (!idNode.isTextual() || !durationNode.isTextual()) {
                continue;
            }
            Duration duration;
            try {
                duration = Duration.parse(durationNode.asText());
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "too short, min 3 min.");
            }
            if (duration.compareTo(Duration.ofMinutes(3)) > 0) {
                eligibleIds.add(idNode.asText());
            }
        }
        List<JsonNode> eligibleVideos = new ArrayList<>();
        for (JsonNode item : items) {
            if (eligibleIds.contains(item.path("id").path("videoId").asText())) {

            eligibleVideos.add(item);}
        }
        if  (eligibleVideos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }


        int randomindex = (int ) (Math.random()* eligibleVideos.size());
        JsonNode selectedVideo = eligibleVideos.get(randomindex);

        String videoId = selectedVideo.get("id").get("videoId").asText();
        String videoTitle = HtmlUtils.htmlUnescape(selectedVideo.get("snippet").get("title").asText());
        String videoDescription = selectedVideo.get("snippet").get("description").asText();
        String videoThumbnailUrl = selectedVideo.get("snippet").get("thumbnails").get("maxres").get("url").asText();
        String ChannelTitle = selectedVideo.get("snippet").get("channelTitle").asText();
        String videoUrl= "https://www.youtube.com"
                + "/watch?v="
                + videoId;
        return Map.of("videoUrl", videoUrl,"videoTitle", videoTitle, "ChannelTitle", ChannelTitle, "videoDescription", videoDescription,"videoThumbnailUrl", videoThumbnailUrl);

    }
}
