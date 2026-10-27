package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.ValuationBatchJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValuationBatchJobRepository extends JpaRepository<ValuationBatchJob, Long> {
    Optional<ValuationBatchJob> findFirstByOrderByStartedAtDesc();

    Optional<ValuationBatchJob> findFirstByStatusOrderByStartedAtDesc(ValuationBatchJob.Status status);
}
