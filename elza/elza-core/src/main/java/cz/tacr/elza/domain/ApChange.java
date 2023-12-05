package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "ap_change")
public class ApChange {

    public static final String USER = "user";

    public enum Type {
        AP_CREATE, AP_UPDATE, AP_DELETE, AP_RESTORE, AP_REPLACE, AP_MIGRATE/***/,
        AP_REVALIDATE,
        NAME_CREATE, NAME_UPDATE, NAME_DELETE, NAME_SET_PREFERRED,
        DESC_CREATE, DESC_UPDATE, DESC_DELETE,
        AP_IMPORT, AP_SYNCH
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer changeId;

    @Column(nullable = false)
    private OffsetDateTime changeDate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_20, nullable = true)
    private ApChange.Type type;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId", nullable = true)
    private ApExternalSystem externalSystem;

    public Integer getChangeId() {
        return changeId;
    }

    public void setChangeId(Integer changeId) {
        this.changeId = changeId;
    }

    public OffsetDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(OffsetDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(UsrUser user) {
        this.user = user;
    }

    public ApChange.Type getType() {
        return type;
    }

    public void setType(ApChange.Type type) {
        this.type = type;
    }

    public ApExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(ApExternalSystem externalSystemType) {
        this.externalSystem = externalSystemType;
    }
}
