package com.dairy.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 挤奶班次：某个挤奶位在某天的某一段被哪位挤奶员用。 */
@Entity
@Table(name = "milking_shift")
public class MilkingShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "shift_no", nullable = false, unique = true, length = 32)
    public String shiftNo;

    @Column(name = "milking_date", nullable = false)
    public LocalDate milkingDate;

    /** 早班 / 中班 / 晚班 */
    @Column(nullable = false, length = 16)
    public String period;

    @Column(name = "stall_id", nullable = false)
    public Long stallId;

    /** 这一班挤的是哪个牛舍的牛 */
    @Column(name = "barn_id", nullable = false)
    public Long barnId;

    @Column(nullable = false, length = 32)
    public String milker;

    /** 从 0:00 起算的分钟数 */
    @Column(name = "start_min", nullable = false)
    public Integer startMin;

    @Column(name = "end_min", nullable = false)
    public Integer endMin;

    /** 本班产奶公斤数，收班时才登记 */
    @Column(name = "milk_kg")
    public Double milkKg;

    /** 待开挤 / 挤奶中 / 已完成 / 已取消 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
