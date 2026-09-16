package com.dairy.farm.entity;

import jakarta.persistence.*;

/** 奶牛档案：耳号唯一，一头牛同时只在一个牛舍。 */
@Entity
@Table(name = "cow")
public class Cow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "ear_tag", nullable = false, unique = true, length = 32)
    public String earTag;

    @Column(length = 32)
    public String nickname;

    @Column(length = 32)
    public String breed;

    /** 泌乳中 / 干奶期 / 待产 / 已淘汰 */
    @Column(nullable = false, length = 16)
    public String lactation;

    @Column(name = "barn_id")
    public Long barnId;

    /** 在栏 / 离栏 */
    @Column(nullable = false, length = 16)
    public String status;
}
