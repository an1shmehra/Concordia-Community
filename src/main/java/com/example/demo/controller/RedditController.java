package com.example.demo.controller;

import com.example.demo.model.RedditPost;
import com.example.demo.service.RedditApiService;
import com.example.demo.service.RedditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
    public String getRedditPosts(Model model) {
        List<RedditPost> posts = redditService.getConcordiaPosts();
        model.addAttribute("posts", posts); // send the list to Thymeleaf
        return "reddit"; // Thymeleaf template name: reddit.html
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


