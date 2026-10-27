package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.AirQualityReading;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirQualityReadingRepository extends JpaRepository<AirQualityReading, Long> {
    Optional<AirQualityReading> findFirstByRegionOrderByRecordedAtDesc(String region);
}
