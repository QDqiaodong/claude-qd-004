package com.dairy.farm.repository;

import com.dairy.farm.entity.MilkingStall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkingStallRepository extends JpaRepository<MilkingStall, Long> {
    boolean existsByCode(String code);

    long countByBarnId(Long barnId);
}
