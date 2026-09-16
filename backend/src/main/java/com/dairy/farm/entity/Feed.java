package com.dairy.farm.entity;

import jakarta.persistence.*;

/** 饲料：编号唯一，带库存与预警线。 */
@Entity
@Table(name = "feed")
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 公斤 / 袋 / 捆 */
    @Column(nullable = false, length = 16)
    public String unit;

    @Column(nullable = false)
    public Integer stock;

    @Column(name = "warn_stock", nullable = false)
    public Integer warnStock;

    /** 在用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
