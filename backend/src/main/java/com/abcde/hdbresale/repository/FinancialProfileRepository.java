package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.FinancialProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialProfileRepository extends JpaRepository<FinancialProfile, Long> {
    Optional<FinancialProfile> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
