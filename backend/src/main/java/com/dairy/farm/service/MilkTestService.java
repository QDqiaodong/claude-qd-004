package com.dairy.farm.service;

import com.dairy.farm.dto.BizException;
import com.dairy.farm.entity.MilkTest;
import com.dairy.farm.entity.MilkingShift;
import com.dairy.farm.repository.MilkTestRepository;
import com.dairy.farm.repository.MilkingShiftRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilkTestService {

    /** 扣留、倒掉即结案，结案后不能再插抽检；复检放行下一条抽检。 */
    private static final Set<String> FINAL_DISPOSITIONS = Set.of("扣留", "倒掉");

    private final MilkTestRepository tests;
    private final MilkingShiftRepository shifts;

    public MilkTestService(MilkTestRepository tests, MilkingShiftRepository shifts) {
        this.tests = tests;
        this.shifts = shifts;
    }

    public List<MilkTest> list(Long shiftId) {
        if (shiftId == null) {
            return tests.findAllByOrderByCreatedAtDesc();
        }
        return tests.findByShiftIdOrderByIdAsc(shiftId);
    }

    /**
     * 登记一条抽检：记体细胞数和合格/不合格结论。
     * 只能挂已收班的班次；上一条是未处置的不合格、或已经结案时都不能再插；
     * 只有上一条处置写的是「复检」才允许再抽一次。
     * 按场长定的规矩：不合格不拦下一班，只在这条班次上留处置。
     */
    @Transactional
    public MilkTest addSampling(MilkTest input) {
        if (input.shiftId == null) {
            throw new BizException("抽检要挂到一个已收班的班次上");
        }
        if (input.somaticCells == null || input.somaticCells <= 0) {
            throw new BizException("抽检要记体细胞数");
        }
        String result = "不合格".equals(input.result) ? "不合格"
                : ("合格".equals(input.result) ? "合格" : null);
        if (result == null) {
            throw new BizException("抽检要给结论：合格或不合格");
        }

        MilkingShift shift = shifts.findByIdForUpdate(input.shiftId)
                .orElseThrow(() -> new BizException("班次不存在"));
        if (!"已完成".equals(shift.status)) {
            throw new BizException("班次 " + shift.shiftNo + " 还没（" + shift.status
                    + "），抽检只能挂在已经收班的班次上");
        }

        List<MilkTest> ledger = tests.findByShiftIdOrderByIdAsc(shift.id);
        MilkTest last = ledger.isEmpty() ? null : ledger.get(ledger.size() - 1);
        if (last != null) {
            if ("抽检".equals(last.recordType)) {
                // 合格已结案；不合格必须先留处置，不能连着插第二条抽检
                throw new BizException("合格".equals(last.result)
                        ? "班次 " + shift.shiftNo + " 上一次抽检已合格结案，不能再抽检"
                        : "班次 " + shift.shiftNo + " 上一条抽检不合格，得先留处置（扣留、复检或倒掉），不能再插一条抽检");
            }
            if (!"复检".equals(last.disposition)) {
                throw new BizException("班次 " + shift.shiftNo + " 已经按「"
                        + last.disposition + "」结案，不能再抽检");
            }
            // 上一条处置是「复检」：放行，允许再插一条抽检
        }

        MilkTest saved = new MilkTest();
        saved.shiftId = shift.id;
        saved.recordType = "抽检";
        saved.somaticCells = input.somaticCells;
        saved.result = result;
        saved.operator = blankToNull(input.operator);
        saved.remark = input.remark;
        saved.createdAt = LocalDateTime.now();
        return tests.save(saved);
    }

    /**
     * 给一次不合格抽检留处置：扣留 / 复检 / 倒掉。
     * 只有「复检」放行同一班次的下一条抽检；扣留、倒掉直接结案。
     */
    @Transactional
    public MilkTest addDisposition(MilkTest input) {
        if (input.shiftId == null) {
            throw new BizException("处置要挂到一个已收班的班次上");
        }
        String disposition = normalizeDisposition(input.disposition);
        if (disposition == null) {
            throw new BizException("处置要选一种：扣留、复检或倒掉");
        }

        MilkingShift shift = shifts.findByIdForUpdate(input.shiftId)
                .orElseThrow(() -> new BizException("班次不存在"));
        if (!"已完成".equals(shift.status)) {
            throw new BizException("班次 " + shift.shiftNo + " 还没收班，没有可处置的抽检");
        }

        List<MilkTest> ledger = tests.findByShiftIdOrderByIdAsc(shift.id);
        MilkTest last = ledger.isEmpty() ? null : ledger.get(ledger.size() - 1);
        if (last == null || !"抽检".equals(last.recordType) || !"不合格".equals(last.result)) {
            throw new BizException("班次 " + shift.shiftNo + " 现在没有等处置的不合格抽检，不能凭空留处置");
        }

        MilkTest saved = new MilkTest();
        saved.shiftId = shift.id;
        saved.recordType = "处置";
        saved.disposition = disposition;
        saved.operator = blankToNull(input.operator);
        saved.remark = input.remark;
        saved.createdAt = LocalDateTime.now();
        return tests.save(saved);
    }

    private String normalizeDisposition(String d) {
        if (d == null) {
            return null;
        }
        String v = d.trim();
        return FINAL_DISPOSITIONS.contains(v) || "复检".equals(v) ? v : null;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
