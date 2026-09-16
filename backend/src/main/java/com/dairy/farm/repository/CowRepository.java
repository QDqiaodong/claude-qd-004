package com.dairy.farm.repository;

import com.dairy.farm.entity.Cow;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CowRepository extends JpaRepository<Cow, Long> {

    boolean existsByEarTag(String earTag);

    List<Cow> findByBarnIdAndStatusAndLactation(Long barnId, String status, String lactation);

    long countByBarnIdAndStatus(Long barnId, String status);

    /** 锁牛档案行：淘汰与转舍/摘舍串行化，不能出现淘汰了还占着名额的账。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cow c where c.id = :id")
    Optional<Cow> findByIdForUpdate(@Param("id") Long id);
}
