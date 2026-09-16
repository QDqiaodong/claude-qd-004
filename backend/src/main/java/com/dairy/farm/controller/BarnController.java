package com.dairy.farm.controller;

import com.dairy.farm.entity.Barn;
import com.dairy.farm.entity.MilkingStall;
import com.dairy.farm.service.BarnService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BarnController {

    private final BarnService service;

    public BarnController(BarnService service) {
        this.service = service;
    }

    @GetMapping("/barns")
    public List<Barn> listBarns(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String keyword) {
        return service.listBarns(status, keyword);
    }

    @PostMapping("/barns")
    public Barn createBarn(@RequestBody Barn input) {
        return service.createBarn(input);
    }

    @PutMapping("/barns/{id}")
    public Barn updateBarn(@PathVariable Long id, @RequestBody Barn input) {
        return service.updateBarn(id, input);
    }

    @GetMapping("/stalls")
    public List<MilkingStall> listStalls(@RequestParam(required = false) Long barnId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        return service.listStalls(barnId, status, keyword);
    }

    @PostMapping("/stalls")
    public MilkingStall createStall(@RequestBody MilkingStall input) {
        return service.createStall(input);
    }

    @PutMapping("/stalls/{id}")
    public MilkingStall updateStall(@PathVariable Long id, @RequestBody MilkingStall input) {
        return service.updateStall(id, input);
    }
}
