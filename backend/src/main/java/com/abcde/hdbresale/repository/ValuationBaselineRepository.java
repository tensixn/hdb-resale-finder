package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.ValuationBaseline;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValuationBaselineRepository extends JpaRepository<ValuationBaseline, Long> {
    Optional<ValuationBaseline> findByTownAndFlatType(String town, String flatType);
}
