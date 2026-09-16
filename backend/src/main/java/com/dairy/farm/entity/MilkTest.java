package com.dairy.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 原奶抽检台账：一条流水要么是一次抽检（记体细胞和合格/不合格结论），
 * 要么是一次处置（扣留 / 复检 / 倒掉）。只能挂在已收班（已完成）的班次上。
 */
@Entity
@Table(name = "milk_test")
public class MilkTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "shift_id", nullable = false)
    public Long shiftId;

    /** 抽检 / 处置 */
    @Column(name = "record_type", nullable = false, length = 16)
    public String recordType;

    /** 体细胞数（个/mL），抽检时必填 */
    @Column(name = "somatic_cells")
    public Long somaticCells;

    /** 合格 / 不合格，抽检时必填 */
    @Column(length = 8)
    public String result;

    /** 扣留 / 复检 / 倒掉，处置时必填 */
    @Column(length = 8)
    public String disposition;

    @Column(length = 32)
    public String operator;

    @Column(length = 255)
    public String remark;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
