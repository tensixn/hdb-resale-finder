package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.WeightProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightProfileRepository extends JpaRepository<WeightProfile, Long> {
    Optional<WeightProfile> findByUserId(Long userId);
}
