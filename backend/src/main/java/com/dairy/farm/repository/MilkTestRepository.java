package com.dairy.farm.repository;

import com.dairy.farm.entity.MilkTest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkTestRepository extends JpaRepository<MilkTest, Long> {

    List<MilkTest> findAllByOrderByCreatedAtDesc();

    List<MilkTest> findByShiftIdOrderByIdAsc(Long shiftId);
}
