package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.abcde.hdbresale.config.GrantRules;
import com.abcde.hdbresale.domain.MaritalStatus;
import com.abcde.hdbresale.repository.FinancialProfileRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-11 and FR-12: grant tiers, singles rules, PHG proximity, loan maths and affordability. */
class CpfGrantServiceTest {

    private final GrantRules rules = new GrantRules("test", "test",
            new GrantRules.CpfHousingGrant(16000, 8000, 80000, 50000, 40000, 25000),
            new GrantRules.Ehg(9000, List.of(new GrantRules.Band(1500, 120000), new GrantRules.Band(5000, 65000),
                    new GrantRules.Band(9000, 5000))),
            new GrantRules.Phg(4, 30000, 20000, 15000, 10000),
            new GrantRules.Loan("HDB", 2.6, 25, 25, 75), 30);
    private final CpfGrantService service = new CpfGrantService(rules, mock(FlatRepository.class),
            mock(FinancialProfileRepository.class), new PostalSectorDirectory());

    @Test
    void ehgFollowsTheIncomeBands() {
        assertThat(service.ehg(1200, MaritalStatus.MARRIED).amount()).isEqualTo(120000);
        assertThat(service.ehg(4800, MaritalStatus.ENGAGED).amount()).isEqualTo(65000);
        assertThat(service.ehg(9000, MaritalStatus.MARRIED).amount()).isEqualTo(5000);
        assertThat(service.ehg(9001, MaritalStatus.MARRIED).amount()).isZero();
    }

    @Test
    void singlesGetHalfTheGrantAtDoubleTheirIncome() {
        assertThat(service.ehg(700, MaritalStatus.SINGLE).amount()).isEqualTo(60000);
        assertThat(service.ehg(2400, MaritalStatus.SINGLE).amount()).isEqualTo(32500);
        assertThat(service.ehg(4600, MaritalStatus.SINGLE).amount()).isZero();
    }

    @Test
    void phgDependsOnLivingArrangementAndDistance() {
        assertThat(service.phg(MaritalStatus.MARRIED, true, null, null).amount()).isEqualTo(30000);
        assertThat(service.phg(MaritalStatus.MARRIED, false, 3.9, "560442").amount()).isEqualTo(20000);
        assertThat(service.phg(MaritalStatus.SINGLE, false, 2.0, "560442").amount()).isEqualTo(10000);
        assertThat(service.phg(MaritalStatus.MARRIED, false, 4.1, "560442").amount()).isZero();
        assertThat(service.phg(MaritalStatus.MARRIED, false, null, null).amount()).isZero();
    }

    @Test
    void instalmentMatchesTheAmortisationFormula() {
        // $400,000 over 25 years at 2.6% is about $1,814.68 a month.
        assertThat(CpfGrantService.monthlyInstalment(400_000, 2.6, 25)).isCloseTo(1814.68, org.assertj.core.data.Offset.offset(0.5));
        assertThat(CpfGrantService.monthlyInstalment(0, 2.6, 25)).isZero();
        assertThat(CpfGrantService.monthlyInstalment(120_000, 0, 10)).isEqualTo(1000);
    }

    @Test
    void affordabilityUsesTheMortgageServicingRatio() {
        assertThat(service.affordability(1000, 6000).rating()).isEqualTo("COMFORTABLE");
        assertThat(service.affordability(1700, 6000).rating()).isEqualTo("STRETCHED");
        assertThat(service.affordability(2000, 6000).rating()).isEqualTo("ABOVE_LIMIT");
        assertThat(service.affordability(2000, 0).rating()).isEqualTo("UNKNOWN");
    }
}
