package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.AccessType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity(name = "ap_change")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ApChange {
    @Id
    @GeneratedValue
    @AccessType(AccessType.Type.PROPERTY)
    private Integer changeId;

    @Column(nullable = false)
    private LocalDateTime changeDate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = true)
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

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
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

    public enum Type {
        VARIANT_NAME_DELETE, ACCESS_POINT_DELETE, VARIANT_NAME_UPDATE, VARIANT_NAME_CREATE, ACCESS_POINT_CREATE, ACCESS_POINT_UPDATE;
    }
}
