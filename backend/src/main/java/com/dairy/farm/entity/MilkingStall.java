package com.dairy.farm.entity;

import jakarta.persistence.*;

/** 挤奶位：编号唯一，归属某个牛舍，停用或维修时不能再排班次。 */
@Entity
@Table(name = "milking_stall")
public class MilkingStall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    @Column(name = "barn_id")
    public Long barnId;

    /** 并列式 / 转盘式 */
    @Column(name = "stall_type", nullable = false, length = 16)
    public String stallType;

    /** 可用 / 停用 / 维修 */
    @Column(nullable = false, length = 16)
    public String status;
}
