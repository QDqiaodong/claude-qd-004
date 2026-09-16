package com.dairy.farm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 饲料领用流水：给某个牛舍领料，多领了退料。 */
@Entity
@Table(name = "feed_issue")
public class FeedIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "barn_id", nullable = false)
    public Long barnId;

    @Column(name = "feed_id", nullable = false)
    public Long feedId;

    @Column(nullable = false)
    public Integer qty;

    /** 领用 / 退料 */
    @Column(nullable = false, length = 16)
    public String kind;

    @Column(length = 32)
    public String operator;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
