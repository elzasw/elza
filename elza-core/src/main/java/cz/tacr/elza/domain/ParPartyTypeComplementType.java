package cz.tacr.elza.domain;

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
public class ParPartyTypeComplementType implements cz.tacr.elza.api.ParPartyTypeComplementType<ParPartyType, ParComplementType> {

    @Id
    @GeneratedValue
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

    @Override
    public Integer getPartyTypeComplementTypeId() {
        return partyTypeComplementTypeId;
    }

    @Override
    public void setPartyTypeComplementTypeId(final Integer parPartyTypeComplementTypeId) {
        this.partyTypeComplementTypeId = parPartyTypeComplementTypeId;
    }

    @Override
    public ParComplementType getComplementType() {
        return complementType;
    }

    @Override
    public void setComplementType(final ParComplementType complementType) {
        this.complementType = complementType;
    }

    @Override
    public ParPartyType getPartyType() {
        return partyType;
    }

    @Override
    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
    }

    @Override
    public boolean isRepeatable() {
        return repeatable;
    }

    @Override
	public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartyTypeComplementType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.ParPartyTypeComplementType other = (cz.tacr.elza.api.ParPartyTypeComplementType) obj;

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
