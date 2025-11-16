package com.example.demo.controller;

import com.example.demo.model.RedditPost;
import com.example.demo.service.RedditApiService;
import com.example.demo.service.RedditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/reddit")
public class RedditController {

    private final RedditApiService redditApiService;
    private final RedditService redditService;

    public RedditController(RedditApiService redditApiService, RedditService redditService) {
        this.redditApiService = redditApiService;
        this.redditService = redditService;
    }

    @GetMapping("/concordia")
    public String getConcordiaPosts() {
        return redditApiService.getConcordiaPosts();
    }

    @GetMapping("/posts")
    public String getRedditPosts(@RequestParam(value = "q", required = false) String query, Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts();

        String trimmed = (query != null) ? query.trim() : null;
        if (trimmed != null && !trimmed.isEmpty()) {
            String lowerQuery = trimmed.toLowerCase();

            posts = posts.stream()
                    .filter(post ->
                            (post.getTitle() != null && post.getTitle().toLowerCase().contains(lowerQuery)) ||
                                    (post.getSelftext() != null && post.getSelftext().toLowerCase().contains(lowerQuery))
                    )
                    .collect(Collectors.toList());

            if (posts.isEmpty()) {
                model.addAttribute("notFoundMessage", "Not found");
            }
        }

        model.addAttribute("posts", posts);
        // Keep the input value after searching; clear it when blank/cleared
        model.addAttribute("query", (trimmed != null && !trimmed.isEmpty()) ? trimmed : null);
        return "reddit";
    }


    @GetMapping("/{index}")
    public String postDetails(@PathVariable int index, Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts();
        if (index >= 0 && index < posts.size()) {
            RedditPost post = posts.get(index);
            model.addAttribute("post", post);
            return "reddit-details"; // new Thymeleaf template
        } else {
            return "redirect:/reddit"; // invalid index
        }
    }
}


