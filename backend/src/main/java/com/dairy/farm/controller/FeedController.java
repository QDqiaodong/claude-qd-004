package com.dairy.farm.controller;

import com.dairy.farm.entity.Feed;
import com.dairy.farm.entity.FeedIssue;
import com.dairy.farm.service.FeedService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FeedController {

    private final FeedService service;

    public FeedController(FeedService service) {
        this.service = service;
    }

    @GetMapping("/feeds")
    public List<Feed> list(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        return service.list(keyword, status);
    }

    @PostMapping("/feeds")
    public Feed create(@RequestBody Feed input) {
        return service.create(input);
    }

    @PutMapping("/feeds/{id}")
    public Feed update(@PathVariable Long id, @RequestBody Feed input) {
        return service.update(id, input);
    }

    @GetMapping("/feed-issues")
    public List<FeedIssue> issues(@RequestParam(required = false) Long barnId) {
        return service.issueList(barnId);
    }

    @PostMapping("/feed-issues")
    public FeedIssue issue(@RequestBody FeedIssue input) {
        return service.issue(input);
    }
}
