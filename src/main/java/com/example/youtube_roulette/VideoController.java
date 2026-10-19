package com.example.youtube_roulette;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;



@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class VideoController {
    @GetMapping("/videos/random")
    public Map<String,String> keywort(@RequestParam String keyword) {
     return  videoService.getRandomVideo(keyword);
    }

    private final VideoService videoService;
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }
}
