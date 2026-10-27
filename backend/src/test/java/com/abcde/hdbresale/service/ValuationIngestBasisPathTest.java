package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abcde.hdbresale.config.AppProperties;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.GeoPoint;
import com.abcde.hdbresale.domain.Town;
import com.abcde.hdbresale.domain.Transaction;
import com.abcde.hdbresale.gateway.ExternalApiGateway;
import com.abcde.hdbresale.gateway.GeocodeResult;
import com.abcde.hdbresale.gateway.ResaleRecord;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.TownRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.repository.ValuationBaselineRepository;
import com.abcde.hdbresale.repository.ValuationBatchJobRepository;
import com.abcde.hdbresale.service.ValuationService.IngestResult;
import com.abcde.hdbresale.service.strategy.ValuationStrategy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lab 4 section 3.2.2: basis path testing of {@link ValuationService#ingest} (UC-12, FR-29), which takes new
 * data.gov.sg resale records, skips or flags bad ones, matches each to a known flat (or geocodes a new one) and
 * stores transactions it has not seen before.
 *
 * <p>Six decisions (P1 more records, P2 town missing, P3 required field invalid, P4 flat unknown, P5 flat could not
 * be created, P6 transaction is new) give V(G) = 6 + 1 = 7. The flow graph and path list are in
 * docs/lab4/Lab4-Test-Report.md (WB-B1 to WB-B7). Each path is driven with a list of zero or one record.
 */
class ValuationIngestBasisPathTest {

    private final FlatRepository flats = mock(FlatRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final TownRepository towns = mock(TownRepository.class);
    private final ExternalApiGateway gateway = mock(ExternalApiGateway.class);
    private ValuationService service;
    private Flat known;

    private static ResaleRecord record(String town, String block, long price) {
        return new ResaleRecord("2026-09", town, "4 ROOM", block, "ANG MO KIO AVE 10", "07 TO 09", 92, 1980, price);
    }

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null, new AppProperties.Valuation(5, 6, 3, "0 0 3 * * *"), null,
                null, false);
        service = new ValuationService(flats, transactions, mock(ValuationBaselineRepository.class),
                mock(ValuationBatchJobRepository.class), towns, gateway, mock(ValuationStrategy.class), props,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));

        when(towns.findAll()).thenReturn(List.of(new Town("ANG MO KIO", 1.37, 103.845, "north")));
        known = new Flat("406", "ANG MO KIO AVE 10", "ANG MO KIO", "4 ROOM", 92, "07 TO 09", 1980, 1.37, 103.845, "north");
        known.applyTransaction("2026-05", 480_000);
        ReflectionTestUtils.setField(known, "id", 1L);
        when(flats.findByBlockAndStreetAndFlatTypeAndStoreyRange(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(flats.findByBlockAndStreetAndFlatTypeAndStoreyRange("406", "ANG MO KIO AVE 10", "4 ROOM", "07 TO 09"))
                .thenReturn(Optional.of(known));
        when(flats.save(any(Flat.class))).thenAnswer(inv -> {
            Flat f = inv.getArgument(0);
            if (f.getId() == null) {
                ReflectionTestUtils.setField(f, "id", 2L);
            }
            return f;
        });
    }

    @Test
    @DisplayName("WB-B1  path 1-2-11: no records -> nothing inserted, nothing flagged")
    void path1NoRecords() {
        IngestResult r = service.ingest(List.of());
        assertThat(r.inserted()).isZero();
        assertThat(r.incompleteTowns()).isEmpty();
    }

    @Test
    @DisplayName("WB-B2  path 1-2-3-2-11: record without a town -> skipped silently")
    void path2BlankTown() {
        IngestResult r = service.ingest(List.of(record(" ", "406", 500_000)));
        assertThat(r.inserted()).isZero();
        assertThat(r.incompleteTowns()).isEmpty();
        verify(transactions, never()).save(any());
    }

    @Test
    @DisplayName("WB-B3  path 1-2-3-4-5-2-11: price of 0 -> town flagged incomplete, nothing stored")
    void path3IncompleteRecord() {
        IngestResult r = service.ingest(List.of(record("ANG MO KIO", "406", 0)));
        assertThat(r.inserted()).isZero();
        assertThat(r.incompleteTowns()).containsExactly("ANG MO KIO");
        verify(transactions, never()).save(any());
    }

    @Test
    @DisplayName("WB-B4  path 1-2-3-4-6-8-9-10-2-11: known flat, new transaction -> stored, latest price updated")
    void path4KnownFlatNewTransaction() {
        IngestResult r = service.ingest(List.of(record("ANG MO KIO", "406", 510_000)));
        assertThat(r.inserted()).isEqualTo(1);
        assertThat(known.getLatestPrice()).isEqualTo(510_000);
        assertThat(known.getLatestMonth()).isEqualTo("2026-09");
        verify(transactions).save(any(Transaction.class));
        verify(gateway, never()).geocode(anyString());
    }

    @Test
    @DisplayName("WB-B5  path 1-2-3-4-6-8-9-2-11: known flat, transaction already stored -> not duplicated")
    void path5DuplicateTransaction() {
        when(transactions.existsByFlatIdAndMonthAndResalePrice(1L, "2026-09", 510_000)).thenReturn(true);
        IngestResult r = service.ingest(List.of(record("ANG MO KIO", "406", 510_000)));
        assertThat(r.inserted()).isZero();
        assertThat(known.getLatestPrice()).isEqualTo(480_000);
        verify(transactions, never()).save(any());
    }

    @Test
    @DisplayName("WB-B6  path 1-2-3-4-6-7-8-9-10-2-11: unknown flat, exact geocode -> flat created and transaction stored")
    void path6NewFlatCreated() {
        when(gateway.geocode("999 ANG MO KIO AVE 10"))
                .thenReturn(Optional.of(new GeocodeResult("999 AMK", new GeoPoint(1.371, 103.846), false)));
        IngestResult r = service.ingest(List.of(record("ANG MO KIO", "999", 520_000)));
        assertThat(r.inserted()).isEqualTo(1);
        verify(transactions).save(any(Transaction.class));
    }

    @Test
    @DisplayName("WB-B7  path 1-2-3-4-6-7-8-2-11: unknown flat, only an approximate location -> skipped")
    void path7NewFlatNotPlaceable() {
        when(gateway.geocode(anyString()))
                .thenReturn(Optional.of(new GeocodeResult("sector centre", new GeoPoint(1.36, 103.845), true)));
        IngestResult r = service.ingest(List.of(record("ANG MO KIO", "999", 520_000)));
        assertThat(r.inserted()).isZero();
        assertThat(r.incompleteTowns()).isEmpty();
        verify(flats, never()).save(any());
        verify(transactions, never()).existsByFlatIdAndMonthAndResalePrice(anyLong(), anyString(), anyLong());
    }
}
