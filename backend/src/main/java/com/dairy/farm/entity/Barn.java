package com.dairy.farm.entity;

import jakarta.persistence.*;

/** 牛舍：编号唯一，停用前要把栏里的牛安置好。 */
@Entity
@Table(name = "barn")
public class Barn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 犊牛舍 / 产奶舍 / 干奶舍 / 隔离舍 */
    @Column(nullable = false, length = 16)
    public String kind;

    /** 可容纳头数 */
    @Column(nullable = false)
    public Integer capacity;

    /** 在用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
