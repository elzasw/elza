package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import cz.tacr.elza.domain.enumeration.StringLength;
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

/**
 * One person who participated on a binding revision in the external system.
 * Multiple rows can exist per {@link ApBindingState} — typically one per role
 * per person.
 */
@Entity(name = "ap_binding_participant")
public class ApBindingParticipant {

    public enum Role {
        AUTHOR,
        APPROVAL,
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingParticipantId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBindingState.class)
    @JoinColumn(name = "bindingStateId", nullable = false)
    private ApBindingState bindingState;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer bindingStateId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private Role role;

    @Column(nullable = false)
    private OffsetDateTime lastChange;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_250)
    private String institutionCode;

    public Integer getBindingParticipantId() {
        return bindingParticipantId;
    }

    public void setBindingParticipantId(Integer bindingParticipantId) {
        this.bindingParticipantId = bindingParticipantId;
    }

    public ApBindingState getBindingState() {
        return bindingState;
    }

    public void setBindingState(ApBindingState bindingState) {
        this.bindingState = bindingState;
    }

    public Integer getBindingStateId() {
        return bindingStateId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public OffsetDateTime getLastChange() {
        return lastChange;
    }

    public void setLastChange(OffsetDateTime lastChange) {
        this.lastChange = lastChange;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }
}
