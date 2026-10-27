package com.abcde.hdbresale.bootstrap;

import com.abcde.hdbresale.domain.CrimeRecord;
import com.abcde.hdbresale.domain.DensityRecord;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.School;
import com.abcde.hdbresale.domain.Town;
import com.abcde.hdbresale.domain.Transaction;
import com.abcde.hdbresale.repository.CrimeRecordRepository;
import com.abcde.hdbresale.repository.DensityRecordRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.SchoolRepository;
import com.abcde.hdbresale.repository.TownRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.service.LivabilityDataService;
import com.abcde.hdbresale.service.ValuationService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Start-up class: loads the bundled reference data on an empty database (assumption A-5: crime, density and
 * schools come from static CSV files) and a sample of resale transactions. Transaction months are shifted so
 * the newest sample month is the current month, which keeps the rolling 6-month window populated whenever
 * the app is run.
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TownRepository townRepository;
    private final CrimeRecordRepository crimeRepository;
    private final DensityRecordRepository densityRepository;
    private final SchoolRepository schoolRepository;
    private final FlatRepository flatRepository;
    private final TransactionRepository transactionRepository;
    private final ValuationService valuationService;
    private final LivabilityDataService livabilityDataService;
    private final TransactionTemplate tx;

    public DataSeeder(TownRepository townRepository, CrimeRecordRepository crimeRepository,
                      DensityRecordRepository densityRepository, SchoolRepository schoolRepository,
                      FlatRepository flatRepository, TransactionRepository transactionRepository,
                      ValuationService valuationService, LivabilityDataService livabilityDataService,
                      TransactionTemplate tx) {
        this.townRepository = townRepository;
        this.crimeRepository = crimeRepository;
        this.densityRepository = densityRepository;
        this.schoolRepository = schoolRepository;
        this.flatRepository = flatRepository;
        this.transactionRepository = transactionRepository;
        this.valuationService = valuationService;
        this.livabilityDataService = livabilityDataService;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (flatRepository.count() > 0) {
            return;
        }
        log.info("Empty database: loading sample data");
        int year = Year.now().getValue();
        tx.executeWithoutResult(s -> {
            try {
                read("seed/towns.csv", r -> {
                    String town = r.get("town");
                    townRepository.save(new Town(town, d(r.get("latitude")), d(r.get("longitude")), r.get("region")));
                    crimeRepository.save(new CrimeRecord(town, d(r.get("crime_rate_per_1000")), year - 1));
                    densityRepository.save(new DensityRecord(town, d(r.get("density_per_ha")), year - 1));
                });
                read("seed/schools.csv", r -> schoolRepository.save(new School(r.get("name"), r.get("town"),
                        d(r.get("latitude")), d(r.get("longitude")))));

                Map<String, Flat> flats = new HashMap<>();
                read("seed/flats.csv", r -> {
                    flats.put(r.get("id"), flatRepository.save(new Flat(r.get("block"), r.get("street"), r.get("town"),
                            r.get("flat_type"), d(r.get("floor_area_sqm")), r.get("storey_range"),
                            Integer.parseInt(r.get("lease_commence_year")), d(r.get("latitude")), d(r.get("longitude")),
                            r.get("region"))));
                });
                List<Map<String, String>> rows = new ArrayList<>();
                read("seed/transactions.csv", rows::add);
                YearMonth newest = rows.stream().map(r -> YearMonth.parse(r.get("month"))).max(YearMonth::compareTo).orElseThrow();
                long shift = ChronoUnit.MONTHS.between(newest, YearMonth.now(ValuationService.SGT));
                for (Map<String, String> r : rows) {
                    Flat f = flats.get(r.get("flat_id"));
                    String month = YearMonth.parse(r.get("month")).plusMonths(shift).toString();
                    long price = Long.parseLong(r.get("resale_price"));
                    transactionRepository.save(new Transaction(f.getId(), month, price, "SEED"));
                    f.applyTransaction(month, price);
                }
                flatRepository.saveAll(flats.values());
            } catch (IOException e) {
                throw new IllegalStateException("Could not read seed data", e);
            }
        });
        livabilityDataService.reloadStaticData();
        valuationService.refreshBaselines(java.util.Set.of());
        log.info("Loaded {} flats and {} transactions", flatRepository.count(), transactionRepository.count());
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }

    /** Minimal CSV reader: the seed files have no quoted commas except in school names, which are handled. */
    private static void read(String path, Consumer<Map<String, String>> sink) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new ClassPathResource(path).getInputStream(),
                StandardCharsets.UTF_8))) {
            String[] header = in.readLine().split(",");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = splitCsv(line);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < header.length; i++) {
                    row.put(header[i], i < cells.size() ? cells.get(i) : "");
                }
                sink.accept(row);
            }
        }
    }

    private static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
