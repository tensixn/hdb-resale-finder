package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.Bookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Bookmark> findByUserIdAndFlatId(Long userId, Long flatId);

    boolean existsByUserIdAndFlatId(Long userId, Long flatId);
}
