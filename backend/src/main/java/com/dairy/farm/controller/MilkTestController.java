package com.dairy.farm.controller;

import com.dairy.farm.entity.MilkTest;
import com.dairy.farm.service.MilkTestService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milk-tests")
public class MilkTestController {

    private final MilkTestService service;

    public MilkTestController(MilkTestService service) {
        this.service = service;
    }

    /** 抽检台账：不传班次就全量倒序，传了班次按落下先后正序。 */
    @GetMapping
    public List<MilkTest> list(@RequestParam(required = false) Long shiftId) {
        return service.list(shiftId);
    }

    /** 登记一条抽检（体细胞数 + 合格/不合格结论）。 */
    @PostMapping("/samplings")
    public MilkTest sampling(@RequestBody MilkTest input) {
        return service.addSampling(input);
    }

    /** 给不合格抽检留处置（扣留 / 复检 / 倒掉）。 */
    @PostMapping("/dispositions")
    public MilkTest disposition(@RequestBody MilkTest input) {
        return service.addDisposition(input);
    }
}
