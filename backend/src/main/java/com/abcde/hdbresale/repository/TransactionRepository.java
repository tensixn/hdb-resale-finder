package com.abcde.hdbresale.repository;

import com.abcde.hdbresale.domain.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFlatIdOrderByMonthDesc(Long flatId);

    boolean existsByFlatIdAndMonthAndResalePrice(Long flatId, String month, long resalePrice);

    /** Rows for the rolling-average calculation: [town, flatType, price, floorArea] from a month onwards. */
    @Query("""
            select f.town, f.flatType, t.resalePrice, f.floorAreaSqm
            from Transaction t, Flat f
            where t.flatId = f.id and t.month >= :fromMonth
            """)
    List<Object[]> findWindowRows(@Param("fromMonth") String fromMonth);

    @Query("select max(t.month) from Transaction t")
    String findLatestMonth();
}
