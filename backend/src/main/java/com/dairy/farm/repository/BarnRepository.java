package com.dairy.farm.repository;

import com.dairy.farm.entity.Barn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BarnRepository extends JpaRepository<Barn, Long> {
    boolean existsByCode(String code);
}
