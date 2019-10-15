package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Vazba M:N mezi typem osoby a typem doplňku jména.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_type_complement_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyTypeComplementType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partyTypeComplementTypeId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParComplementType.class)
    @JoinColumn(name = "complementTypeId", nullable = false)
    private ParComplementType complementType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    private ParPartyType partyType;

    @Column(nullable = false)
    private boolean repeatable;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getPartyTypeComplementTypeId() {
        return partyTypeComplementTypeId;
    }

    public void setPartyTypeComplementTypeId(final Integer parPartyTypeComplementTypeId) {
        this.partyTypeComplementTypeId = parPartyTypeComplementTypeId;
    }

    public ParComplementType getComplementType() {
        return complementType;
    }

    public void setComplementType(final ParComplementType complementType) {
        this.complementType = complementType;
    }

    public ParPartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyTypeComplementType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyTypeComplementType other = (ParPartyTypeComplementType) obj;

        return new EqualsBuilder().append(partyTypeComplementTypeId, other.getPartyTypeComplementTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyTypeComplementTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyTypeComplementType pk=" + partyTypeComplementTypeId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
