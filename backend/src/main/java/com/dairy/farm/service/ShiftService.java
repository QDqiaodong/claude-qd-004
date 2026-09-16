package com.dairy.farm.service;

import com.dairy.farm.dto.BizException;
import com.dairy.farm.entity.Barn;
import com.dairy.farm.entity.MilkingShift;
import com.dairy.farm.entity.MilkingStall;
import com.dairy.farm.repository.BarnRepository;
import com.dairy.farm.repository.CowRepository;
import com.dairy.farm.repository.MilkingShiftRepository;
import com.dairy.farm.repository.MilkingStallRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private final MilkingShiftRepository shifts;
    private final MilkingStallRepository stalls;
    private final BarnRepository barns;
    private final CowRepository cows;

    public ShiftService(MilkingShiftRepository shifts, MilkingStallRepository stalls,
                        BarnRepository barns, CowRepository cows) {
        this.shifts = shifts;
        this.stalls = stalls;
        this.barns = barns;
        this.cows = cows;
    }

    public List<MilkingShift> list(LocalDate date, String status, Long stallId, String milker) {
        List<MilkingShift> all = (date == null)
                ? shifts.findAllByOrderByUpdatedAtDesc()
                : shifts.findByMilkingDateOrderByStartMinAsc(date);
        return all.stream()
                .filter(s -> status == null || status.isEmpty() || status.equals(s.status))
                .filter(s -> stallId == null || stallId.equals(s.stallId))
                .filter(s -> milker == null || milker.isEmpty() || milker.equals(s.milker))
                .toList();
    }

    private String nextShiftNo() {
        long n = shifts.count() + 1;
        String no;
        do {
            no = "MS-" + String.format("%04d", n++);
        } while (shifts.existsByShiftNo(no));
        return no;
    }

    @Transactional
    public MilkingShift open(MilkingShift input) {
        if (input.milkingDate == null) {
            throw new BizException("请选挤奶日期");
        }
        if (input.stallId == null) {
            throw new BizException("请选一个挤奶位");
        }
        if (input.barnId == null) {
            throw new BizException("请选这一班挤哪个牛舍的牛");
        }
        if (input.milker == null || input.milker.isBlank()) {
            throw new BizException("要写清楚这一班是谁挤");
        }
        if (input.startMin == null || input.endMin == null || input.endMin <= input.startMin) {
            throw new BizException("结束时间必须晚于开始时间");
        }

        MilkingStall stall = stalls.findById(input.stallId)
                .orElseThrow(() -> new BizException("挤奶位不存在"));
        if (!"可用".equals(stall.status)) {
            throw new BizException("挤奶位 " + stall.name + " 现在是" + stall.status + "，不能排班");
        }
        Barn barn = barns.findById(input.barnId).orElseThrow(() -> new BizException("牛舍不存在"));
        if (!"在用".equals(barn.status)) {
            throw new BizException("牛舍 " + barn.name + " 已经停用，不能给它排挤奶班次");
        }
        if (cows.findByBarnIdAndStatusAndLactation(barn.id, "在栏", "泌乳中").isEmpty()) {
            throw new BizException("牛舍 " + barn.name + " 现在没有在栏的泌乳牛，这一班不用排");
        }

        String milker = input.milker.trim();
        for (MilkingShift other : shifts.findByStallIdAndMilkingDateAndStatusNotIn(
                stall.id, input.milkingDate, List.of("已取消"))) {
            if (overlap(other, input)) {
                throw new BizException("挤奶位 " + stall.name + " 这个时段已经被班次 "
                        + other.shiftNo + " 占了");
            }
        }
        for (MilkingShift other : shifts.findByMilkerAndMilkingDateAndStatusNotIn(
                milker, input.milkingDate, List.of("已取消"))) {
            if (overlap(other, input)) {
                throw new BizException("挤奶员 " + milker + " 这个时段已经排了班次 "
                        + other.shiftNo + "，一个人不能同时守两个位");
            }
        }

        MilkingShift saved = new MilkingShift();
        saved.shiftNo = nextShiftNo();
        saved.milkingDate = input.milkingDate;
        saved.period = (input.period == null || input.period.isBlank()) ? "早班" : input.period;
        saved.stallId = stall.id;
        saved.barnId = barn.id;
        saved.milker = milker;
        saved.startMin = input.startMin;
        saved.endMin = input.endMin;
        saved.status = "待开挤";
        saved.createdAt = LocalDateTime.now();
        saved.updatedAt = saved.createdAt;
        return shifts.save(saved);
    }

    private boolean overlap(MilkingShift other, MilkingShift input) {
        if (other.startMin == null || other.endMin == null) {
            return false;
        }
        return other.startMin < input.endMin && input.startMin < other.endMin;
    }

    /** 待开挤 -> 挤奶中 -> 已完成；收班必须登记产量。 */
    @Transactional
    public MilkingShift advance(Long id, String action, Double milkKg) {
        MilkingShift shift = shifts.findById(id).orElseThrow(() -> new BizException("班次不存在"));

        if ("start".equals(action)) {
            if (!"待开挤".equals(shift.status)) {
                throw new BizException("只有待开挤的班次能开挤，这一班现在是 " + shift.status);
            }
            shift.status = "挤奶中";
        } else if ("done".equals(action)) {
            if (!"挤奶中".equals(shift.status)) {
                throw new BizException("只有挤奶中的班次能收班，这一班现在是 " + shift.status);
            }
            if (milkKg == null || milkKg <= 0) {
                throw new BizException("收班要登记这一班的产奶公斤数");
            }
            shift.milkKg = milkKg;
            shift.status = "已完成";
        } else if ("cancel".equals(action)) {
            if ("已完成".equals(shift.status)) {
                throw new BizException("已经收班的班次不能取消");
            }
            if ("已取消".equals(shift.status)) {
                throw new BizException("这一班已经取消过了");
            }
            shift.status = "已取消";
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        shift.updatedAt = LocalDateTime.now();
        return shifts.save(shift);
    }
}
