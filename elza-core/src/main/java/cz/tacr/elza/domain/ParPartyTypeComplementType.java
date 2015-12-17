package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.api.ParPartyPartyTypeComplementType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

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


/**
 * Vazba M:N mezi typem osoby a typem doplňku jména.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_type_complement_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyTypeComplementType implements ParPartyPartyTypeComplementType<ParPartyType, ParComplementType> {

    @Id
    @GeneratedValue
    private Integer parPartyTypeComplementTypeId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParComplementType.class)
    @JoinColumn(name = "complementTypeId", nullable = false)
    private ParComplementType complementType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    private ParPartyType partyType;


    @Override
    public Integer getParPartyTypeComplementTypeId() {
        return parPartyTypeComplementTypeId;
    }

    @Override
    public void setParPartyTypeComplementTypeId(final Integer parPartyTypeComplementTypeId) {
        this.parPartyTypeComplementTypeId = parPartyTypeComplementTypeId;
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
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyPartyTypeComplementType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyPartyTypeComplementType other = (ParPartyPartyTypeComplementType) obj;

        return new EqualsBuilder().append(parPartyTypeComplementTypeId, other.getParPartyTypeComplementTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(parPartyTypeComplementTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyTypeComplementType pk=" + parPartyTypeComplementTypeId;
    }
}
