package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Doplňky jmen osob.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_name_complement")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyNameComplement implements cz.tacr.elza.api.ParPartyNameComplement<ParComplementType, ParPartyName> {

    /* Konstanty pro vazby a fieldy. */
    public static final String PARTY_NAME_COMPLEMENT_ID = "partyNameComplementId";

    @Id
    @GeneratedValue
    private Integer partyNameComplementId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParComplementType.class)
    @JoinColumn(name = "complementTypeId", nullable = false)
    private ParComplementType complementType;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParPartyName.class)
    @JoinColumn(name = "partyNameId", nullable = false)
    private ParPartyName partyName;

    @Column(length = StringLength.LENGTH_1000)
    private String complement;


    @Override
    public Integer getPartyNameComplementId() {
        return partyNameComplementId;
    }

    @Override
    public void setPartyNameComplementId(final Integer partyNameComplementId) {
        this.partyNameComplementId = partyNameComplementId;
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
    public ParPartyName getPartyName() {
        return partyName;
    }

    @Override
    public void setPartyName(final ParPartyName partyName) {
        this.partyName = partyName;
    }

    @Override
    public String getComplement() {
        return complement;
    }

    @Override
    public void setComplement(final String complement) {
        this.complement = complement;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParUnitdate)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.ParPartyNameComplement<ParComplementType, ParPartyName> other = (cz.tacr.elza.api.ParPartyNameComplement<ParComplementType, ParPartyName>) obj;

        return new EqualsBuilder().append(partyNameComplementId, other.getPartyNameComplementId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyNameComplementId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyNameComplement pk=" + partyNameComplementId;
    }
}
