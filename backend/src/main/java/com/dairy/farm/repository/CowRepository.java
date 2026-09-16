package com.dairy.farm.repository;

import com.dairy.farm.entity.Cow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CowRepository extends JpaRepository<Cow, Long> {

    boolean existsByEarTag(String earTag);

    List<Cow> findByBarnIdAndStatusAndLactation(Long barnId, String status, String lactation);

    long countByBarnIdAndStatus(Long barnId, String status);
}
