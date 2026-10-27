package com.abcde.hdbresale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.abcde.hdbresale.config.GrantRules;
import com.abcde.hdbresale.domain.CpfGrant;
import com.abcde.hdbresale.domain.Flat;
import com.abcde.hdbresale.domain.MaritalStatus;
import com.abcde.hdbresale.dto.CpfGrantRequestDto;
import com.abcde.hdbresale.dto.CpfGrantResponseDto;
import com.abcde.hdbresale.dto.FinancialProfileDto;
import com.abcde.hdbresale.exception.ApiException;
import com.abcde.hdbresale.repository.FinancialProfileRepository;
import com.abcde.hdbresale.repository.FlatRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Lab 4 section 3.2.1: black-box testing of the control class {@link CpfGrantService} (UC-06, UC-07, FR-10 to FR-13)
 * using equivalence class partitioning and boundary value analysis. Test ids (BB-xx) match the test case table in
 * docs/lab4/Lab4-Test-Report.md. Expected values were worked out from the SRS and grant-rules.yml, not from the
 * code.
 *
 * <p>Fixture: one 4-room flat, last sold for $500,000, at (1.3700, 103.8450) in Ang Mo Kio. Parent postal code
 * 560123 (sector 56) is about 1.1 km away; 640123 (sector 64, Jurong) is about 14.3 km away. The grant rules are
 * the real ones shipped in grant-rules.yml.
 */
class CpfGrantServiceBlackBoxTest {

    private static final long FLAT_ID = 1L;
    private static final String NEAR = "560123";
    private static final String FAR = "640123";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private CpfGrantService service;

    @BeforeEach
    void setUp() throws Exception {
        Flat flat = new Flat("406", "ANG MO KIO AVE 10", "ANG MO KIO", "4 ROOM", 92, "07 TO 09", 1980, 1.3700, 103.8450,
                "north");
        flat.applyTransaction("2026-08", 500_000);
        ReflectionTestUtils.setField(flat, "id", FLAT_ID);
        FlatRepository flats = mock(FlatRepository.class);
        when(flats.findById(anyLong())).thenReturn(Optional.empty());
        when(flats.findById(FLAT_ID)).thenReturn(Optional.of(flat));
        service = new CpfGrantService(shippedGrantRules(), flats, mock(FinancialProfileRepository.class),
                new PostalSectorDirectory());
    }

    /** Binds the real grant-rules.yml so the tests check the configuration the app actually runs with. */
    static GrantRules shippedGrantRules() throws Exception {
        StandardEnvironment env = new StandardEnvironment();
        new YamlPropertySourceLoader().load("grant-rules", new ClassPathResource("grant-rules.yml"))
                .forEach(env.getPropertySources()::addLast);
        return Binder.get(env).bind("grants", GrantRules.class).get();
    }

    private static FinancialProfileDto profile(Long income, MaritalStatus status, String postal, boolean withParents) {
        return new FinancialProfileDto(income, status, postal, withParents);
    }

    private static CpfGrantRequestDto request(FinancialProfileDto p, Integer tenure, Double down, Double rate) {
        return new CpfGrantRequestDto(FLAT_ID, p, false, tenure, down, rate);
    }

    private static long grant(CpfGrantResponseDto r, String scheme) {
        return r.grants().stream().filter(g -> g.scheme().equals(scheme)).mapToLong(CpfGrant::amount).sum();
    }

    // ---- Valid equivalence classes and their boundaries, through estimate() --------------------------------

    static Stream<Arguments> validCases() {
        return Stream.of(
                // id, income, status, postal, withParents, tenure, down%, rate%, CHG, EHG, PHG, downPayment, loan, instalment, rating
                Arguments.of("BB-01", 1500L, MaritalStatus.MARRIED, "", false, null, null, null,
                        80_000, 120_000, 0, 125_000, 175_000, 794, "ABOVE_LIMIT"),
                Arguments.of("BB-02", 1501L, MaritalStatus.MARRIED, "", false, null, null, null,
                        80_000, 110_000, 0, 125_000, 185_000, 839, "ABOVE_LIMIT"),
                Arguments.of("BB-03", 9000L, MaritalStatus.MARRIED, "", false, null, null, null,
                        80_000, 5_000, 0, 125_000, 290_000, 1316, "COMFORTABLE"),
                Arguments.of("BB-04", 9001L, MaritalStatus.ENGAGED, NEAR, false, null, null, null,
                        80_000, 0, 20_000, 125_000, 275_000, 1248, "COMFORTABLE"),
                Arguments.of("BB-05", 4500L, MaritalStatus.SINGLE, "", true, null, null, null,
                        40_000, 2_500, 15_000, 125_000, 317_500, 1440, "ABOVE_LIMIT"),
                Arguments.of("BB-06", 4501L, MaritalStatus.SINGLE, "", false, null, null, null,
                        40_000, 0, 0, 125_000, 335_000, 1520, "ABOVE_LIMIT"),
                Arguments.of("BB-07", 0L, MaritalStatus.MARRIED, FAR, false, null, null, null,
                        80_000, 120_000, 0, 125_000, 175_000, 794, "UNKNOWN"),
                Arguments.of("BB-08", 9001L, MaritalStatus.MARRIED, "", false, 5, 0.0, 0.0,
                        80_000, 0, 0, 125_000, 295_000, 4917, "ABOVE_LIMIT"),
                Arguments.of("BB-09", 9001L, MaritalStatus.MARRIED, "", false, 30, 100.0, 10.0,
                        80_000, 0, 0, 500_000, 0, 0, "COMFORTABLE"),
                Arguments.of("BB-10", 9001L, MaritalStatus.MARRIED, "", false, 25, 24.99, 2.6,
                        80_000, 0, 0, 125_000, 295_000, 1338, "COMFORTABLE"),
                Arguments.of("BB-11", 9001L, MaritalStatus.MARRIED, "", false, 25, 25.0, 2.6,
                        80_000, 0, 0, 125_000, 295_000, 1338, "COMFORTABLE"));
    }

    @ParameterizedTest(name = "{0}: income={1}, {2}, postal={3}, withParents={4}, tenure={5}, down={6}, rate={7}")
    @MethodSource("validCases")
    void validInputsProduceTheExpectedEstimate(String id, Long income, MaritalStatus status, String postal,
                                               boolean withParents, Integer tenure, Double down, Double rate,
                                               long chg, long ehg, long phg, long downPayment, long loan, long instalment,
                                               String rating) {
        CpfGrantRequestDto req = request(profile(income, status, postal, withParents), tenure, down, rate);
        assertThat(validator.validate(req)).as(id + " passes input validation").isEmpty();

        CpfGrantResponseDto r = service.estimate(req, null);

        assertThat(grant(r, "CHG")).as(id + " CPF Housing Grant").isEqualTo(chg);
        assertThat(grant(r, "EHG")).as(id + " EHG").isEqualTo(ehg);
        assertThat(grant(r, "PHG")).as(id + " PHG").isEqualTo(phg);
        assertThat(r.totalGrants()).as(id + " total grants").isEqualTo(chg + ehg + phg);
        assertThat(r.downPayment()).as(id + " down payment").isEqualTo(downPayment);
        assertThat(r.loanAmount()).as(id + " loan").isEqualTo(loan);
        assertThat(r.monthlyInstalment()).as(id + " instalment").isEqualTo(instalment);
        assertThat(r.affordability().rating()).as(id + " affordability").isEqualTo(rating);
    }

    @Test
    @DisplayName("BB-01: omitted loan fields fall back to the default assumptions (FR-12)")
    void defaultsAreShownWhenNoLoanFieldsAreGiven() {
        CpfGrantResponseDto r = service.estimate(request(profile(1500L, MaritalStatus.MARRIED, "", false), null, null, null), null);
        assertThat(r.loan().tenureYears()).isEqualTo(25);
        assertThat(r.loan().interestRatePercent()).isEqualTo(2.6);
        assertThat(r.loan().downPaymentPercent()).isEqualTo(25);
        assertThat(r.grants()).filteredOn(g -> g.scheme().equals("PHG"))
                .extracting(CpfGrant::reason).first().asString().contains("Add your parents' postal code");
    }

    @Test
    @DisplayName("BB-04 / BB-07: parent distance is reported and decides the PHG")
    void parentDistanceIsReported() {
        CpfGrantResponseDto near = service.estimate(request(profile(9001L, MaritalStatus.ENGAGED, NEAR, false), null, null, null), null);
        assertThat(near.parentDistanceKm()).isEqualTo(1.1);
        CpfGrantResponseDto far = service.estimate(request(profile(0L, MaritalStatus.MARRIED, FAR, false), null, null, null), null);
        assertThat(far.parentDistanceKm()).isEqualTo(14.3);
        assertThat(far.grants()).filteredOn(g -> g.scheme().equals("PHG"))
                .extracting(CpfGrant::reason).first().asString().contains("more than 4 km");
    }

    @Test
    @DisplayName("BB-08 / BB-10 / BB-11: a down payment below 25% is raised, and the user is told; 25% is not")
    void downPaymentBelowTheMinimumIsRaisedWithANotice() {
        String notice = "down payment was raised to 25%";
        assertThat(service.estimate(request(profile(9001L, MaritalStatus.MARRIED, "", false), 5, 0.0, 0.0), null)
                .disclaimers()).anyMatch(d -> d.contains(notice));
        assertThat(service.estimate(request(profile(9001L, MaritalStatus.MARRIED, "", false), 25, 24.99, 2.6), null)
                .disclaimers()).anyMatch(d -> d.contains(notice));
        assertThat(service.estimate(request(profile(9001L, MaritalStatus.MARRIED, "", false), 25, 25.0, 2.6), null)
                .disclaimers()).noneMatch(d -> d.contains(notice));
    }

    // ---- Invalid equivalence classes rejected by input validation (HTTP 422 at the API) --------------------

    static Stream<Arguments> invalidInputs() {
        FinancialProfileDto ok = profile(5000L, MaritalStatus.MARRIED, "", false);
        return Stream.of(
                Arguments.of("BB-12", request(profile(-1L, MaritalStatus.MARRIED, "", false), null, null, null), "profile.householdIncome"),
                Arguments.of("BB-13", request(profile(1_000_001L, MaritalStatus.MARRIED, "", false), null, null, null), "profile.householdIncome"),
                Arguments.of("BB-14", request(ok, 4, null, null), "loanTenureYears"),
                Arguments.of("BB-15", request(ok, 31, null, null), "loanTenureYears"),
                Arguments.of("BB-16", request(ok, null, -0.01, null), "downPaymentPercent"),
                Arguments.of("BB-17", request(ok, null, 100.01, null), "downPaymentPercent"),
                Arguments.of("BB-18", request(ok, null, null, -0.01), "interestRatePercent"),
                Arguments.of("BB-19", request(ok, null, null, 10.01), "interestRatePercent"),
                Arguments.of("BB-20", request(profile(5000L, MaritalStatus.MARRIED, "12345", false), null, null, null), "profile.parentPostalCode"));
    }

    @ParameterizedTest(name = "{0}: rejected on {2}")
    @MethodSource("invalidInputs")
    void invalidInputsAreRejectedOnTheRightField(String id, CpfGrantRequestDto req, String field) {
        Set<String> fields = validator.validate(req).stream().map(ConstraintViolation::getPropertyPath)
                .map(Object::toString).collect(Collectors.toSet());
        assertThat(fields).as(id).containsExactly(field);
    }

    @Test
    @DisplayName("BB-21: values exactly on each valid boundary are accepted")
    void validBoundariesAreAccepted() {
        Stream.of(request(profile(0L, MaritalStatus.MARRIED, "", false), 5, 0.0, 0.0),
                        request(profile(1_000_000L, MaritalStatus.SINGLE, "560123", false), 30, 100.0, 10.0))
                .forEach(req -> assertThat(validator.validate(req)).isEmpty());
    }

    // ---- Invalid equivalence classes the service itself rejects ---------------------------------------------

    @Test
    @DisplayName("BB-22: a well-formed postal code in an unknown sector is rejected (UC-07.EX.2)")
    void unknownPostalSectorIsRejected() {
        assertThatThrownBy(() -> service.estimate(request(profile(5000L, MaritalStatus.MARRIED, "990000", false), null, null, null), null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(e.getFieldErrors()).containsKey("parentPostalCode");
                });
    }

    @Test
    @DisplayName("BB-23 / BB-24: a missing marital status or income asks for the profile (UC-06.EX.1)")
    void incompleteProfileIsRejected() {
        for (FinancialProfileDto p : new FinancialProfileDto[] {
                profile(5000L, null, "", false), profile(null, MaritalStatus.MARRIED, "", false)}) {
            assertThatThrownBy(() -> service.estimate(request(p, null, null, null), null))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("PROFILE_REQUIRED"));
        }
    }

    @Test
    @DisplayName("BB-25: an unknown flat is a 404")
    void unknownFlatIsNotFound() {
        CpfGrantRequestDto req = new CpfGrantRequestDto(999L, profile(5000L, MaritalStatus.MARRIED, "", false), false, null, null, null);
        assertThatThrownBy(() -> service.estimate(req, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- Boundaries of derived values that estimate() cannot hit exactly with real postal sectors -----------

    @Test
    @DisplayName("BB-26: PHG proximity boundary at 4 km")
    void phgProximityBoundary() {
        assertThat(service.phg(MaritalStatus.MARRIED, false, 4.0, NEAR).amount()).isEqualTo(20_000);
        assertThat(service.phg(MaritalStatus.MARRIED, false, 4.01, NEAR).amount()).isZero();
    }

    @Test
    @DisplayName("BB-27: affordability boundaries at 25% and 30% of income (MSR)")
    void affordabilityBoundaries() {
        assertThat(service.affordability(2500, 10_000).rating()).isEqualTo("COMFORTABLE");
        assertThat(service.affordability(2510, 10_000).rating()).isEqualTo("STRETCHED");
        assertThat(service.affordability(3000, 10_000).rating()).isEqualTo("STRETCHED");
        assertThat(service.affordability(3010, 10_000).rating()).isEqualTo("ABOVE_LIMIT");
    }

    @Test
    @DisplayName("BB-28: CPF Housing Grant income ceilings and flat size")
    void cpfHousingGrantBoundaries() {
        assertThat(service.cpfHousingGrant(16_000, MaritalStatus.MARRIED, "4 ROOM").amount()).isEqualTo(80_000);
        assertThat(service.cpfHousingGrant(16_001, MaritalStatus.MARRIED, "4 ROOM").amount()).isZero();
        assertThat(service.cpfHousingGrant(8_000, MaritalStatus.SINGLE, "4 ROOM").amount()).isEqualTo(40_000);
        assertThat(service.cpfHousingGrant(8_001, MaritalStatus.SINGLE, "4 ROOM").amount()).isZero();
        assertThat(service.cpfHousingGrant(5_000, MaritalStatus.ENGAGED, "5 ROOM").amount()).isEqualTo(50_000);
        assertThat(service.cpfHousingGrant(5_000, MaritalStatus.SINGLE, "EXECUTIVE").amount()).isEqualTo(25_000);
        assertThat(service.cpfHousingGrant(5_000, MaritalStatus.MARRIED, "2 ROOM").amount()).isEqualTo(80_000);
    }
}
