package com.dairy.farm.service;

import com.dairy.farm.dto.BizException;
import com.dairy.farm.entity.Barn;
import com.dairy.farm.entity.MilkingStall;
import com.dairy.farm.repository.BarnRepository;
import com.dairy.farm.repository.CowRepository;
import com.dairy.farm.repository.MilkingShiftRepository;
import com.dairy.farm.repository.MilkingStallRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarnService {

    private static final List<String> OPEN_SHIFT = List.of("待开挤", "挤奶中");

    private final BarnRepository barns;
    private final MilkingStallRepository stalls;
    private final CowRepository cows;
    private final MilkingShiftRepository shifts;

    public BarnService(BarnRepository barns, MilkingStallRepository stalls,
                       CowRepository cows, MilkingShiftRepository shifts) {
        this.barns = barns;
        this.stalls = stalls;
        this.cows = cows;
        this.shifts = shifts;
    }

    public List<Barn> listBarns(String status, String keyword) {
        return barns.findAll().stream()
                .filter(b -> status == null || status.isEmpty() || status.equals(b.status))
                .filter(b -> keyword == null || keyword.isEmpty()
                        || b.name.contains(keyword) || b.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Barn createBarn(Barn input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("牛舍编号不能为空");
        }
        if (barns.existsByCode(input.code)) {
            throw new BizException("牛舍编号 " + input.code + " 已经存在");
        }
        Barn saved = new Barn();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "产奶舍" : input.kind;
        saved.capacity = input.capacity == null ? 0 : input.capacity;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return barns.save(saved);
    }

    @Transactional
    public Barn updateBarn(Long id, Barn input) {
        Barn barn = barns.findById(id).orElseThrow(() -> new BizException("牛舍不存在"));
        if (input.name != null) {
            barn.name = input.name;
        }
        if (input.kind != null && !input.kind.isBlank()) {
            barn.kind = input.kind;
        }
        // capacity 实体上有默认值 0，只有 >0 才算真的要改
        if (input.capacity != null && input.capacity > 0 && !input.capacity.equals(barn.capacity)) {
            long inBarn = cows.countByBarnIdAndStatus(barn.id, "在栏");
            if (input.capacity < inBarn) {
                throw new BizException("这个舍现在栏里有 " + inBarn + " 头牛，可容纳头数不能改到比它小");
            }
            barn.capacity = input.capacity;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(barn.status)) {
            if (!"在用".equals(input.status)) {
                long inBarn = cows.countByBarnIdAndStatus(barn.id, "在栏");
                if (inBarn > 0) {
                    throw new BizException("牛舍 " + barn.name + " 栏里还有 " + inBarn
                            + " 头在栏的牛，先把它们转舍或者登记离栏再停用");
                }
                if (!shifts.findByBarnIdAndMilkingDateAndStatusNotIn(
                        barn.id, LocalDate.now(), List.of("已取消", "已完成")).isEmpty()) {
                    throw new BizException("牛舍 " + barn.name + " 今天还有没结束的挤奶班次，先处理完再停用");
                }
            }
            barn.status = input.status;
        }
        return barns.save(barn);
    }

    public List<MilkingStall> listStalls(Long barnId, String status, String keyword) {
        return stalls.findAll().stream()
                .filter(s -> barnId == null || barnId.equals(s.barnId))
                .filter(s -> status == null || status.isEmpty() || status.equals(s.status))
                .filter(s -> keyword == null || keyword.isEmpty()
                        || s.name.contains(keyword) || s.code.contains(keyword))
                .toList();
    }

    @Transactional
    public MilkingStall createStall(MilkingStall input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("挤奶位编号不能为空");
        }
        if (stalls.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的挤奶位用掉了");
        }
        if (input.barnId != null && !barns.existsById(input.barnId)) {
            throw new BizException("归属的牛舍不存在");
        }
        MilkingStall saved = new MilkingStall();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.barnId = input.barnId;
        saved.stallType = (input.stallType == null || input.stallType.isBlank()) ? "并列式" : input.stallType;
        saved.status = (input.status == null || input.status.isBlank()) ? "可用" : input.status;
        return stalls.save(saved);
    }

    @Transactional
    public MilkingStall updateStall(Long id, MilkingStall input) {
        MilkingStall stall = stalls.findById(id).orElseThrow(() -> new BizException("挤奶位不存在"));
        if (input.name != null) {
            stall.name = input.name;
        }
        if (input.stallType != null && !input.stallType.isBlank()) {
            stall.stallType = input.stallType;
        }
        if (input.barnId != null && !input.barnId.equals(stall.barnId)) {
            if (!barns.existsById(input.barnId)) {
                throw new BizException("要挪过去的牛舍不存在");
            }
            if (!openShifts(stall.id).isEmpty()) {
                throw new BizException("挤奶位 " + stall.name + " 今天还有没结束的班次，先处理完再改归属");
            }
            stall.barnId = input.barnId;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(stall.status)) {
            if (!"可用".equals(input.status) && !openShifts(stall.id).isEmpty()) {
                throw new BizException("挤奶位 " + stall.name + " 今天还有没结束的班次，先把班次处理完再改成"
                        + input.status);
            }
            stall.status = input.status;
        }
        return stalls.save(stall);
    }

    private List<com.dairy.farm.entity.MilkingShift> openShifts(Long stallId) {
        return shifts.findByStallIdAndMilkingDateAndStatusNotIn(
                stallId, LocalDate.now(), List.of("已取消", "已完成"));
    }
}
