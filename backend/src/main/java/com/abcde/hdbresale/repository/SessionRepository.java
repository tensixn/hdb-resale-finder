package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, String> {
}
