package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Password policy.
 *
 * The table holds exactly one row, created by the migration. For each rule,
 * null or a value <= 0 means the rule is off.
 */
@Entity(name = "usr_policy")
public class UsrPolicy {

    /**
     * ID of the only row.
     */
    public static final int POLICY_ID = 1;

    @Id
    private Integer policyId;

    /**
     * Password validity in days.
     */
    @Column
    private Integer passwordExpiryDays;

    /**
     * Minimal password length.
     */
    @Column
    private Integer passwordMinLength;

    /**
     * Minimal number of character groups (lowercase, uppercase, digits, other), 1–4.
     */
    @Column
    private Integer passwordMinCharGroups;

    public Integer getPolicyId() {
        return policyId;
    }

    public void setPolicyId(final Integer policyId) {
        this.policyId = policyId;
    }

    public Integer getPasswordExpiryDays() {
        return passwordExpiryDays;
    }

    public void setPasswordExpiryDays(final Integer passwordExpiryDays) {
        this.passwordExpiryDays = passwordExpiryDays;
    }

    public Integer getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(final Integer passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public Integer getPasswordMinCharGroups() {
        return passwordMinCharGroups;
    }

    public void setPasswordMinCharGroups(final Integer passwordMinCharGroups) {
        this.passwordMinCharGroups = passwordMinCharGroups;
    }
}
