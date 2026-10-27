package com.abcde.hdbresale.domain;

import com.abcde.hdbresale.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity: the Home Buyer's saved financial details (UC-07). Income and the parental postal code are
 * encrypted at rest with AES-256-GCM through {@link EncryptedStringConverter} (NFR-6).
 */
@Entity
@Table(name = "financial_profile")
public class FinancialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "household_income", nullable = false)
    private String householdIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", nullable = false)
    private MaritalStatus maritalStatus;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "parent_postal_code")
    private String parentPostalCode;

    @Column(name = "living_with_parents", nullable = false)
    private boolean livingWithParents;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FinancialProfile() {
    }

    public FinancialProfile(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public long getHouseholdIncome() { return Long.parseLong(householdIncome); }
    public void setHouseholdIncome(long income) { this.householdIncome = Long.toString(income); }
    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getParentPostalCode() { return parentPostalCode; }
    public void setParentPostalCode(String parentPostalCode) { this.parentPostalCode = parentPostalCode; }
    public boolean isLivingWithParents() { return livingWithParents; }
    public void setLivingWithParents(boolean livingWithParents) { this.livingWithParents = livingWithParents; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
