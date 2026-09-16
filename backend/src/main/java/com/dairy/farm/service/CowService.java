package com.dairy.farm.service;

import com.dairy.farm.dto.BizException;
import com.dairy.farm.entity.Barn;
import com.dairy.farm.entity.Cow;
import com.dairy.farm.repository.BarnRepository;
import com.dairy.farm.repository.CowRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CowService {

    private final CowRepository cows;
    private final BarnRepository barns;

    public CowService(CowRepository cows, BarnRepository barns) {
        this.cows = cows;
        this.barns = barns;
    }

    public List<Cow> list(Long barnId, String status, String lactation, String keyword) {
        return cows.findAll().stream()
                .filter(c -> barnId == null || barnId.equals(c.barnId))
                .filter(c -> status == null || status.isEmpty() || status.equals(c.status))
                .filter(c -> lactation == null || lactation.isEmpty() || lactation.equals(c.lactation))
                .filter(c -> keyword == null || keyword.isEmpty()
                        || c.earTag.contains(keyword)
                        || (c.nickname != null && c.nickname.contains(keyword)))
                .toList();
    }

    @Transactional
    public Cow create(Cow input) {
        if (input.earTag == null || input.earTag.isBlank()) {
            throw new BizException("耳号不能为空");
        }
        if (cows.existsByEarTag(input.earTag)) {
            throw new BizException("耳号 " + input.earTag + " 已经登记过了");
        }
        Barn barn = null;
        if (input.barnId != null) {
            barn = barns.findById(input.barnId).orElseThrow(() -> new BizException("牛舍不存在"));
            if (!"在用".equals(barn.status)) {
                throw new BizException("牛舍 " + barn.name + " 已经停用，不能往里放牛");
            }
        }
        Cow saved = new Cow();
        saved.earTag = input.earTag.trim();
        saved.nickname = input.nickname;
        saved.breed = input.breed;
        saved.lactation = (input.lactation == null || input.lactation.isBlank()) ? "泌乳中" : input.lactation;
        saved.status = (input.status == null || input.status.isBlank()) ? "在栏" : input.status;
        if (barn != null) {
            if ("在栏".equals(saved.status)) {
                checkRoom(barn);
            }
            saved.barnId = barn.id;
        }
        return cows.save(saved);
    }

    @Transactional
    public Cow update(Long id, Cow input) {
        Cow cow = cows.findById(id).orElseThrow(() -> new BizException("这头牛不在档案里"));
        if ("已淘汰".equals(cow.lactation) && input.lactation != null && !"已淘汰".equals(input.lactation)) {
            throw new BizException("耳号 " + cow.earTag + " 已经淘汰了，不能改回泌乳状态");
        }
        if (input.nickname != null) {
            cow.nickname = input.nickname;
        }
        if (input.breed != null) {
            cow.breed = input.breed;
        }
        if (input.lactation != null && !input.lactation.isBlank()) {
            cow.lactation = input.lactation;
        }
        if (input.barnId != null && !input.barnId.equals(cow.barnId)) {
            Barn target = barns.findById(input.barnId)
                    .orElseThrow(() -> new BizException("要转过去的牛舍不存在"));
            if (!"在用".equals(target.status)) {
                throw new BizException("牛舍 " + target.name + " 已经停用，不能往那转");
            }
            if ("在栏".equals(cow.status)) {
                checkRoom(target);
            }
            cow.barnId = target.id;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(cow.status)) {
            cow.status = input.status;
            if ("离栏".equals(input.status)) {
                cow.barnId = null;
            }
        }
        return cows.save(cow);
    }

    private void checkRoom(Barn barn) {
        long inBarn = cows.countByBarnIdAndStatus(barn.id, "在栏");
        if (barn.capacity > 0 && inBarn >= barn.capacity) {
            throw new BizException("牛舍 " + barn.name + " 最多容纳 " + barn.capacity
                    + " 头，现在已经有 " + inBarn + " 头了，转不进去");
        }
    }
}
