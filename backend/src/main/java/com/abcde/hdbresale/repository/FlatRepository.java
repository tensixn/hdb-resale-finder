package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.Flat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlatRepository extends JpaRepository<Flat, Long> {

    /**
     * Candidate flats for a search (UC-01 step 5). Empty type or town lists mean "any"; the
     * booleans avoid passing empty IN lists, which some databases reject.
     */
    @Query("""
            select f from Flat f
            where f.latestPrice is not null
              and (:minPrice is null or f.latestPrice >= :minPrice)
              and (:maxPrice is null or f.latestPrice <= :maxPrice)
              and (:anyType = true or f.flatType in :types)
              and (:anyTown = true or f.town in :towns)
            """)
    List<Flat> findCandidates(@Param("minPrice") Long minPrice, @Param("maxPrice") Long maxPrice,
                              @Param("anyType") boolean anyType, @Param("types") Collection<String> types,
                              @Param("anyTown") boolean anyTown, @Param("towns") Collection<String> towns);

    Optional<Flat> findByBlockAndStreetAndFlatTypeAndStoreyRange(String block, String street, String flatType,
                                                                 String storeyRange);

    @Query("select distinct f.town from Flat f order by f.town")
    List<String> findTowns();
}
