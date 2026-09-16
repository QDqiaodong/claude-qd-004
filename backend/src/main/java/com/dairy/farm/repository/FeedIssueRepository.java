package com.dairy.farm.repository;

import com.dairy.farm.entity.FeedIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedIssueRepository extends JpaRepository<FeedIssue, Long> {

    List<FeedIssue> findAllByOrderByCreatedAtDesc();

    List<FeedIssue> findByBarnIdOrderByCreatedAtAsc(Long barnId);

    List<FeedIssue> findByBarnIdAndFeedId(Long barnId, Long feedId);
}
