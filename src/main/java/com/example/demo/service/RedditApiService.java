package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RedditApiService {

    @Value("${reddit.user.agent}")
    private String userAgent;

    private final RedditAuthService authService;

    public RedditApiService(RedditAuthService authService) {
        this.authService = authService;
    }

    public String getConcordiaPosts() {
        String accessToken = authService.getAccessToken();

        String url = "https://oauth.reddit.com/r/concordia/new?limit=20";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("User-Agent", userAgent);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        return response.getBody();
    }
}
