package com.abcde.hdbresale.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the {@code app.*} settings in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Search search, Valuation valuation, External external,
                            List<String> corsOrigins, boolean allowManualRefresh) {

    /**
     * @param jwtSecret        HMAC key for access tokens (at least 32 bytes)
     * @param encryptionKey    base64 AES-256 key for sensitive columns (NFR-6)
     * @param tokenTtl         access token lifetime, renewed while active (TBD-5)
     * @param sessionMax       absolute session limit (TBD-5)
     * @param maxFailedLogins  failed logins before lockout (TBD-4)
     * @param lockDuration     lockout length (TBD-4)
     */
    public record Security(String jwtSecret, String encryptionKey, Duration tokenTtl, Duration sessionMax,
                           int maxFailedLogins, Duration lockDuration) {
    }

    /** @param maxResults cap on ranked results (NFR-1 is sized for 500) */
    public record Search(int maxResults, Duration resultSetTtl) {
    }

    /**
     * @param fairBandPercent  +/- band around the estate average treated as Fair-Valued (TBD-1)
     * @param windowMonths     rolling window length (FR-9)
     * @param minSampleSize    fewer transactions than this means no estimate (UC-05.EX.1)
     * @param refreshCron      schedule for the 24-hour batch (FR-28)
     */
    public record Valuation(double fairBandPercent, int windowMonths, int minSampleSize, String refreshCron) {
    }

    /**
     * @param mode      "offline" uses the bundled simulators; "live" calls the real APIs
     * @param timeout   per-call timeout (NFR-3)
     */
    public record External(String mode, Duration timeout, OneMap onemap, Lta lta, DataGov datagov) {
        public boolean live() {
            return "live".equalsIgnoreCase(mode);
        }
    }

    public record OneMap(String baseUrl, String email, String password) {
    }

    public record Lta(String baseUrl, String accountKey) {
    }

    public record DataGov(String baseUrl, String resaleResourceId, String psiUrl) {
    }
}
