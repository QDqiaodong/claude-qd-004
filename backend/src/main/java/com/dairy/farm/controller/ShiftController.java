package com.dairy.farm.controller;

import com.dairy.farm.entity.MilkingShift;
import com.dairy.farm.service.ShiftService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService service;

    public ShiftController(ShiftService service) {
        this.service = service;
    }

    @GetMapping
    public List<MilkingShift> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long stallId,
            @RequestParam(required = false) String milker) {
        return service.list(date, status, stallId, milker);
    }

    @PostMapping
    public MilkingShift open(@RequestBody MilkingShift input) {
        return service.open(input);
    }

    @PostMapping("/{id}/advance")
    public MilkingShift advance(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) Double milkKg) {
        return service.advance(id, action, milkKg);
    }
}
