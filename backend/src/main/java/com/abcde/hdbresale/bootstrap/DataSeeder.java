package com.abcde.hdbresale.bootstrap;

import com.abcde.hdbresale.domain.Transaction;
import com.abcde.hdbresale.repository.CrimeRecordRepository;
import com.abcde.hdbresale.repository.DensityRecordRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import com.abcde.hdbresale.repository.SchoolRepository;
import com.abcde.hdbresale.repository.TownRepository;
import com.abcde.hdbresale.repository.TransactionRepository;
import com.abcde.hdbresale.service.LivabilityDataService;
import com.abcde.hdbresale.service.ValuationService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
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
        // TODO (Lab 4): implement run
    }
}
