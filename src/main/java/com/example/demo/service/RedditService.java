package com.example.demo.service;
import com.example.demo.model.RedditPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedditService {

    @Value("${reddit.access-token}")
    private String accessToken;

    @Value("${reddit.user.agent}")
    private String userAgent;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RedditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<RedditPost> getConcordiaPosts() {
        String url = "https://oauth.reddit.com/r/concordia/new"; // or /hot, /top

        // Set headers
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("User-Agent", userAgent);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        // Make the GET request
        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
        );

        List<RedditPost> posts = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode children = root.path("data").path("children");

            for (JsonNode child : children) {
                JsonNode postData = child.path("data");
                RedditPost post = new RedditPost(
                        postData.path("title").asText(),
                        postData.path("author").asText(),
                        postData.path("url").asText(),
                        postData.path("selftext").asText()
                );
                posts.add(post);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return posts;
    }
}
