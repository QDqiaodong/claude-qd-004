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

    /** 泌乳状态里只有淘汰是封档状态：已淘汰 = 离栏 + 不占任何牛舍名额，三者必须同时成立。 */
    private static final String CULLED = "已淘汰";
    private static final String IN_BARN = "在栏";
    private static final String LEFT_BARN = "离栏";

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
        boolean culled = CULLED.equals(input.lactation);
        Barn barn = null;
        if (input.barnId != null && !culled) {
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
        saved.status = culled ? LEFT_BARN
                : ((input.status == null || input.status.isBlank()) ? IN_BARN : input.status);
        // 淘汰牛不允许带着牛舍名额建档，耳号也由 uk_cow_ear_tag 挡着不能重复登记
        if (culled || LEFT_BARN.equals(saved.status)) {
            saved.barnId = null;
        } else {
            if (barn != null) {
                checkRoom(barn);
            }
            saved.barnId = barn == null ? null : barn.id;
        }
        return cows.save(saved);
    }

    /**
     * 登记淘汰（会计规矩）。
     * 改状态和腾名额是一笔账：泌乳状态置「已淘汰」、在栏状态置「离栏」、牛舍名额摘掉（barn_id 置空）
     * 必须在同一个事务里一起落库，只改状态不腾名额、或只腾名额还挂着泌乳中都过不去。
     * 中途断开时整笔回滚，不会出现已经淘汰却还占着名额的账。
     */
    @Transactional
    public Cow cull(Long id) {
        Cow cow = cows.findByIdForUpdate(id).orElseThrow(() -> new BizException("这头牛不在档案里"));
        if (CULLED.equals(cow.lactation)) {
            throw new BizException("耳号 " + cow.earTag + " 已经淘汰离舍了，不能重复淘汰");
        }
        cow.lactation = CULLED;
        cow.status = LEFT_BARN;
        cow.barnId = null;
        return cows.save(cow);
    }

    @Transactional
    public Cow update(Long id, Cow input) {
        Cow cow = cows.findByIdForUpdate(id).orElseThrow(() -> new BizException("这头牛不在档案里"));

        // 淘汰即封档：这头牛不能再填回任何一间舍，也不能改回泌乳/在栏，只留昵称、品种可改
        if (CULLED.equals(cow.lactation)) {
            if ((input.lactation != null && !input.lactation.isBlank() && !CULLED.equals(input.lactation))
                    || (input.status != null && !input.status.isBlank() && !LEFT_BARN.equals(input.status))
                    || input.barnId != null) {
                throw new BizException("耳号 " + cow.earTag
                        + " 已经淘汰离舍，档案已封档：不能再分回牛舍，也不能改回泌乳/在栏");
            }
            if (input.nickname != null) {
                cow.nickname = input.nickname;
            }
            if (input.breed != null) {
                cow.breed = input.breed;
            }
            return cows.save(cow);
        }

        // 没淘汰的牛，不许在普通保存里就地把泌乳状态改成淘汰：淘汰必须走 cull()，由系统同时摘掉名额
        if (CULLED.equals(input.lactation)) {
            throw new BizException("淘汰要走【登记淘汰】：系统会在同一笔账里把它从牛舍名额中摘掉，不能只在档案上改个状态");
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

        // 先算出这笔保存之后的牛舍和在栏状态，再统一验容量，保证占名额的账算准
        Long targetBarnId = cow.barnId;
        if (input.barnId != null && !input.barnId.equals(cow.barnId)) {
            Barn target = barns.findById(input.barnId)
                    .orElseThrow(() -> new BizException("要转过去的牛舍不存在"));
            if (!"在用".equals(target.status)) {
                throw new BizException("牛舍 " + target.name + " 已经停用，不能往那转");
            }
            targetBarnId = target.id;
        }
        String targetStatus = cow.status;
        if (input.status != null && !input.status.isBlank()) {
            targetStatus = input.status;
        }
        if (LEFT_BARN.equals(targetStatus)) {
            targetBarnId = null;
        }
        if (IN_BARN.equals(targetStatus) && targetBarnId != null && !targetBarnId.equals(cow.barnId)) {
            Barn target = barns.findById(targetBarnId)
                    .orElseThrow(() -> new BizException("要转过去的牛舍不存在"));
            checkRoom(target);
        }
        cow.barnId = targetBarnId;
        cow.status = targetStatus;
        return cows.save(cow);
    }

    private void checkRoom(Barn barn) {
        long inBarn = cows.countByBarnIdAndStatus(barn.id, IN_BARN);
        if (barn.capacity > 0 && inBarn >= barn.capacity) {
            throw new BizException("牛舍 " + barn.name + " 最多容纳 " + barn.capacity
                    + " 头，现在已经有 " + inBarn + " 头了，转不进去");
        }
    }
}
