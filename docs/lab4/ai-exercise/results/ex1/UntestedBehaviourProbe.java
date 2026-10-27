package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.abcde.hdbresale.config.GrantRules;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.MaritalStatus;
import com.abcde.hdbresale.dto.CpfGrantRequestDto;
import com.abcde.hdbresale.dto.FinancialProfileDto;
import com.abcde.hdbresale.repository.FinancialProfileRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** AI-exercise probe: behaviour the graded test suite does not pin down. Not part of the app's suite. */
class UntestedBehaviourProbe {

    private CpfGrantService service(GrantRules rules) {
        Flat flat = new Flat("406", "ANG MO KIO AVE 10", "ANG MO KIO", "4 ROOM", 92, "07 TO 09", 1980, 1.3700, 103.8450, "north");
        flat.applyTransaction("2026-08", 500_000);
        ReflectionTestUtils.setField(flat, "id", 1L);
        FlatRepository flats = mock(FlatRepository.class);
        when(flats.findById(anyLong())).thenReturn(Optional.of(flat));
        return new CpfGrantService(rules, flats, mock(FinancialProfileRepository.class), new PostalSectorDirectory());
    }

    private CpfGrantRequestDto req(String postal, boolean withParents, Double down) {
        return new CpfGrantRequestDto(1L, new FinancialProfileDto(6000L, MaritalStatus.MARRIED, postal, withParents), false, null, down, null);
    }

    @Test
    void P1_parentDistanceIsFlaggedAsApproximate() throws Exception {
        var r = service(CpfGrantServiceBlackBoxTest.shippedGrantRules()).estimate(req("560123", false, null), null);
        assertThat(String.join(" | ", r.disclaimers())).containsIgnoringCase("approximate");
    }

    @Test
    void P2_firstTimerCitizenAssumptionIsStated() throws Exception {
        var r = service(CpfGrantServiceBlackBoxTest.shippedGrantRules()).estimate(req("", false, null), null);
        assertThat(String.join(" | ", r.disclaimers())).containsIgnoringCase("first-time");
    }

    @Test
    void P3_livingWithParentsIgnoresAnUnknownParentPostalCode() throws Exception {
        assertThatCode(() -> service(CpfGrantServiceBlackBoxTest.shippedGrantRules()).estimate(req("999999", true, null), null))
                .doesNotThrowAnyException();
    }

    @Test
    void P4_minimumDownPaymentComesFromLoanToValueNotFromTheDefault() throws Exception {
        GrantRules s = CpfGrantServiceBlackBoxTest.shippedGrantRules();
        GrantRules.Loan l = s.loan();
        GrantRules changed = new GrantRules(s.effectiveFrom(), s.source(), s.cpfHousingGrant(), s.ehg(), s.phg(),
                new GrantRules.Loan(l.lender(), l.interestRatePercent(), l.tenureYears(), 30, l.maxLoanToValuePercent()), s.msrPercent());
        var r = service(changed).estimate(req("", false, 25.0), null);
        assertThat(r.loan().downPaymentPercent()).isEqualTo(25.0);
    }
}
