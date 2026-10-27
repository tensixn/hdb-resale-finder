package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.CrimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrimeRecordRepository extends JpaRepository<CrimeRecord, Long> {
}
