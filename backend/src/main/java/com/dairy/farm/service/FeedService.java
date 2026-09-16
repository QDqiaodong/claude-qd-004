package com.dairy.farm.service;

import com.dairy.farm.dto.BizException;
import com.dairy.farm.entity.Barn;
import com.dairy.farm.entity.Feed;
import com.dairy.farm.entity.FeedIssue;
import com.dairy.farm.repository.BarnRepository;
import com.dairy.farm.repository.FeedIssueRepository;
import com.dairy.farm.repository.FeedRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    private final FeedRepository feeds;
    private final FeedIssueRepository issues;
    private final BarnRepository barns;

    public FeedService(FeedRepository feeds, FeedIssueRepository issues, BarnRepository barns) {
        this.feeds = feeds;
        this.issues = issues;
        this.barns = barns;
    }

    public List<Feed> list(String keyword, String status) {
        return feeds.findAll().stream()
                .filter(f -> status == null || status.isEmpty() || status.equals(f.status))
                .filter(f -> keyword == null || keyword.isEmpty()
                        || f.name.contains(keyword) || f.code.contains(keyword))
                .toList();
    }

    public List<FeedIssue> issueList(Long barnId) {
        if (barnId == null) {
            return issues.findAllByOrderByCreatedAtDesc();
        }
        return issues.findByBarnIdOrderByCreatedAtAsc(barnId);
    }

    @Transactional
    public Feed create(Feed input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("饲料编号不能为空");
        }
        if (feeds.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的饲料用掉了");
        }
        Feed saved = new Feed();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.unit = (input.unit == null || input.unit.isBlank()) ? "公斤" : input.unit;
        saved.stock = input.stock == null ? 0 : input.stock;
        saved.warnStock = input.warnStock == null ? 0 : input.warnStock;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return feeds.save(saved);
    }

    @Transactional
    public Feed update(Long id, Feed input) {
        Feed feed = feeds.findById(id).orElseThrow(() -> new BizException("饲料不存在"));
        if (input.name != null) {
            feed.name = input.name;
        }
        if (input.unit != null && !input.unit.isBlank()) {
            feed.unit = input.unit;
        }
        if (input.warnStock != null) {
            feed.warnStock = input.warnStock;
        }
        if (input.stock != null && input.stock < 0) {
            throw new BizException("库存不能是负数");
        }
        if (input.stock != null) {
            feed.stock = input.stock;
        }
        if (input.status != null && !input.status.isBlank()) {
            feed.status = input.status;
        }
        return feeds.save(feed);
    }

    /** 领料 / 退料：库存和净领量都在这条链上卡。 */
    @Transactional
    public FeedIssue issue(FeedIssue input) {
        if (input.barnId == null) {
            throw new BizException("请选一个牛舍");
        }
        if (input.feedId == null) {
            throw new BizException("请选一种饲料");
        }
        if (input.qty == null || input.qty <= 0) {
            throw new BizException("数量要大于 0");
        }
        Barn barn = barns.findById(input.barnId).orElseThrow(() -> new BizException("牛舍不存在"));
        Feed feed = feeds.findById(input.feedId).orElseThrow(() -> new BizException("饲料不存在"));

        if (!"在用".equals(feed.status)) {
            throw new BizException("饲料 " + feed.name + " 已经停用，不能再领");
        }
        String kind = "退料".equals(input.kind) ? "退料" : "领用";
        if ("领用".equals(kind)) {
            if (!"在用".equals(barn.status)) {
                throw new BizException("牛舍 " + barn.name + " 已经停用，不能给它领料");
            }
            if (feed.stock < input.qty) {
                throw new BizException("饲料 " + feed.name + " 库存只剩 " + feed.stock
                        + " " + feed.unit + "，不够领 " + input.qty + " " + feed.unit);
            }
            feed.stock = feed.stock - input.qty;
        } else {
            int taken = takenQty(barn.id, feed.id);
            if (taken < input.qty) {
                throw new BizException("牛舍 " + barn.name + " 在 " + feed.name + " 上只领了 "
                        + taken + " " + feed.unit + "，退不了 " + input.qty + " " + feed.unit);
            }
            feed.stock = feed.stock + input.qty;
        }
        feeds.save(feed);

        FeedIssue saved = new FeedIssue();
        saved.barnId = barn.id;
        saved.feedId = feed.id;
        saved.qty = input.qty;
        saved.kind = kind;
        saved.operator = input.operator;
        saved.createdAt = LocalDateTime.now();
        return issues.save(saved);
    }

    private int takenQty(Long barnId, Long feedId) {
        int net = 0;
        for (FeedIssue i : issues.findByBarnIdAndFeedId(barnId, feedId)) {
            net += "领用".equals(i.kind) ? i.qty : -i.qty;
        }
        return net;
    }
}
