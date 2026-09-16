package com.dairy.farm.repository;

import com.dairy.farm.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    boolean existsByCode(String code);
}
