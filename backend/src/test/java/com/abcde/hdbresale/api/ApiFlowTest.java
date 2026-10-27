package com.abcde.hdbresale.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.abcde.hdbresale.bootstrap.Application;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** End-to-end checks of the main use cases through the REST API, against the seeded sample data. */
@SpringBootTest(classes = Application.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:apitest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;

    private JsonNode call(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        String body = mvc.perform(req).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : json.readTree(body);
    }

    private MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private String register(String email) throws Exception {
        JsonNode r = call(postJson("/api/auth/register", Map.of("email", email, "password", "secret123",
                "confirmPassword", "secret123", "consent", true)), 201);
        return "Bearer " + r.get("token").asText();
    }

    @Test
    void searchRanksFlatsAndCanBeFilteredAndReweighted() throws Exception {
        JsonNode r = call(postJson("/api/search", Map.of("workplace1", "Raffles Place", "workplace2", "Jurong East",
                "transportModes", List.of("PUBLIC_TRANSPORT"), "maxPrice", 600000)), 200);
        JsonNode results = r.get("results");
        assertThat(results.size()).isGreaterThan(20);
        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i - 1).get("matchScore").asDouble()).isGreaterThanOrEqualTo(results.get(i).get("matchScore").asDouble());
            assertThat(results.get(i).get("flat").get("price").asLong()).isLessThanOrEqualTo(600000);
        }
        assertThat(results.get(0).get("commute").size()).isEqualTo(2);

        String id = r.get("searchId").asText();
        JsonNode filtered = call(postJson("/api/search/" + id + "/view", Map.of("flatTypes", List.of("3 ROOM"))), 200);
        filtered.get("results").forEach(x -> assertThat(x.get("flat").get("flatType").asText()).isEqualTo("3 ROOM"));

        JsonNode leaseHeavy = call(postJson("/api/search/" + id + "/view",
                Map.of("weights", Map.of("lease", 100, "safety", 0, "density", 0, "airQuality", 0))), 200);
        JsonNode top = leaseHeavy.get("results").get(0);
        assertThat(top.get("livability").get("index").asDouble()).isEqualTo(top.get("livability").get("lease").asDouble());

        JsonNode none = call(postJson("/api/search/" + id + "/view", Map.of("maxPrice", 1000)), 200);
        assertThat(none.get("totalShown").asInt()).isZero();
        assertThat(none.get("suggestions").size()).isPositive();
    }

    @Test
    void searchNeedsWorkplaceOneAndAKnownAddress() throws Exception {
        call(postJson("/api/search", Map.of("workplace1", "")), 422);
        JsonNode bad = call(postJson("/api/search", Map.of("workplace1", "Atlantis")), 422);
        assertThat(bad.get("fieldErrors").has("workplace1")).isTrue();
        call(postJson("/api/search", Map.of("workplace1", "238859")), 200);
    }

    @Test
    void flatDetailShowsValuationAndCommuteOnlyWithASearch() throws Exception {
        JsonNode d = call(get("/api/flats/1"), 200);
        assertThat(d.get("commute").isNull()).isTrue();
        assertThat(d.get("valuation").get("label").asText()).isNotEmpty();
        String id = call(postJson("/api/search", Map.of("workplace1", "one-north")), 200).get("searchId").asText();
        JsonNode withSearch = call(get("/api/flats/1").param("searchId", id), 200);
        assertThat(withSearch.get("commute").size()).isEqualTo(1);
    }

    @Test
    void bookmarksAndCompareNeedSignInAndAtLeastTwoFlats() throws Exception {
        call(get("/api/bookmarks"), 401);
        String auth = register("compare@test.sg");
        call(postJson("/api/bookmarks", Map.of("flatId", 3)).header("Authorization", auth), 201);
        call(postJson("/api/bookmarks", Map.of("flatId", 3)).header("Authorization", auth), 409);
        call(postJson("/api/compare", Map.of("flatIds", List.of(3))).header("Authorization", auth), 422);
        call(postJson("/api/compare", Map.of("flatIds", List.of(3, 4))).header("Authorization", auth), 422);
        call(postJson("/api/bookmarks", Map.of("flatId", 4)).header("Authorization", auth), 201);
        JsonNode cmp = call(postJson("/api/compare", Map.of("flatIds", List.of(3, 4))).header("Authorization", auth), 200);
        assertThat(cmp.get("flats").size()).isEqualTo(2);
        assertThat(cmp.get("best").has("price")).isTrue();
        call(delete("/api/bookmarks/3").header("Authorization", auth), 204);
        assertThat(call(get("/api/bookmarks").header("Authorization", auth), 200).size()).isEqualTo(1);
    }

    @Test
    void financialProfileIsEncryptedAtRestAndDeletable() throws Exception {
        String auth = register("money@test.sg");
        call(put("/api/profile/financial").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"householdIncome\":7345,\"maritalStatus\":\"MARRIED\",\"parentPostalCode\":\"560123\"}"), 200);
        Map<String, Object> raw = jdbc.queryForMap("select household_income, parent_postal_code from financial_profile");
        assertThat(raw.values().toString()).doesNotContain("7345").doesNotContain("560123");
        JsonNode profile = call(get("/api/profile").header("Authorization", auth), 200);
        assertThat(profile.get("financial").get("householdIncome").asLong()).isEqualTo(7345);

        JsonNode cpf = call(postJson("/api/cpf/estimate", Map.of("flatId", 5)).header("Authorization", auth), 200);
        assertThat(cpf.get("grants").size()).isEqualTo(3);

        call(delete("/api/profile/financial").header("Authorization", auth), 204);
        assertThat(jdbc.queryForObject("select count(*) from financial_profile", Integer.class)).isZero();
    }

    @Test
    void accountLocksAfterFiveFailuresAndSignOutEndsTheSession() throws Exception {
        String auth = register("lock@test.sg");
        call(post("/api/auth/logout").header("Authorization", auth), 204);
        call(get("/api/profile").header("Authorization", auth), 401);
        for (int i = 0; i < 4; i++) {
            call(postJson("/api/auth/login", Map.of("email", "lock@test.sg", "password", "wrong-pass1")), 401);
        }
        call(postJson("/api/auth/login", Map.of("email", "lock@test.sg", "password", "wrong-pass1")), 423);
        call(postJson("/api/auth/login", Map.of("email", "lock@test.sg", "password", "secret123")), 423);
    }

    @Test
    void presetWeightsAreNormalisedAndSavedForSignedInUsers() throws Exception {
        String auth = register("preset@test.sg");
        JsonNode w = call(put("/api/livability/weights").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"lease\":66,\"safety\":18,\"density\":18,\"airQuality\":18}"), 200);
        assertThat(w.get("lease").asDouble() + w.get("safety").asDouble() + w.get("density").asDouble()
                + w.get("airQuality").asDouble()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.01));
        assertThat(call(get("/api/livability/weights").header("Authorization", auth), 200).get("lease").asDouble())
                .isCloseTo(55.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void frameworkErrorsKeepTheirStatusInsteadOf500() throws Exception {
        call(get("/api/flats/abc"), 400);
        call(get("/api/no-such-endpoint"), 404);
        call(get("/api/auth/login"), 405);
    }

    @Test
    void manualRefreshIngestsAndRecordsTheRun() throws Exception {
        mvc.perform(post("/api/system/valuation-refresh")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        JsonNode st = call(get("/api/system/status"), 200);
        assertThat(st.get("lastRefreshStatus").asText()).isEqualTo("SUCCESS");
    }
}
