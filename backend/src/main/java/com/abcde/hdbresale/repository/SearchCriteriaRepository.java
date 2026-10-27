package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.SearchCriteria;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCriteriaRepository extends JpaRepository<SearchCriteria, Long> {
    Optional<SearchCriteria> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
