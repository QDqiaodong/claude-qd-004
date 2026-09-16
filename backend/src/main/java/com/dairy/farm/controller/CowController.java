package com.dairy.farm.controller;

import com.dairy.farm.entity.Cow;
import com.dairy.farm.service.CowService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cows")
public class CowController {

    private final CowService service;

    public CowController(CowService service) {
        this.service = service;
    }

    @GetMapping
    public List<Cow> list(@RequestParam(required = false) Long barnId,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String lactation,
                          @RequestParam(required = false) String keyword) {
        return service.list(barnId, status, lactation, keyword);
    }

    @PostMapping
    public Cow create(@RequestBody Cow input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    public Cow update(@PathVariable Long id, @RequestBody Cow input) {
        return service.update(id, input);
    }

    /** 登记淘汰：同事务内置「已淘汰 / 离栏」并摘掉牛舍名额，不分两步做。 */
    @PostMapping("/{id}/cull")
    public Cow cull(@PathVariable Long id) {
        return service.cull(id);
    }
}
