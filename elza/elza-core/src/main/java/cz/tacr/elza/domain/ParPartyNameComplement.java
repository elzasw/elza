package cz.tacr.elza.domain;

import java.util.Comparator;

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
 * @author Martin Kužel
 *         [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_name_complement")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ParPartyNameComplement {

    /* Konstanty pro vazby a fieldy. */
    public static final String PARTY_NAME_COMPLEMENT_ID = "partyNameComplementId";
    public static final String PARTY_NAME_FK = "partyName.partyNameId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partyNameComplementId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParComplementType.class)
    @JoinColumn(name = "complementTypeId", nullable = false)
    private ParComplementType complementType;

    @RestResource(exported = false)
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer complementTypeId;
    
    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParPartyName.class)
    @JoinColumn(name = "partyNameId", nullable = false)
    private ParPartyName partyName;

    @RestResource(exported = false)
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer partyNameId;

    @Column(length = StringLength.LENGTH_1000)
    private String complement;

    public Integer getPartyNameComplementId() {
        return partyNameComplementId;
    }

    public void setPartyNameComplementId(final Integer partyNameComplementId) {
        this.partyNameComplementId = partyNameComplementId;
    }

    public ParComplementType getComplementType() {
        return complementType;
    }

    public void setComplementType(final ParComplementType complementType) {
        this.complementType = complementType;
        this.complementTypeId = complementType != null ? complementType.getComplementTypeId() : null;
    }
    
    public Integer getComplementTypeId() {
        return complementTypeId;
    }

    public ParPartyName getPartyName() {
        return partyName;
    }

    public void setPartyName(final ParPartyName partyName) {
        this.partyName = partyName;
        this.partyNameId = partyName != null ? partyName.getPartyNameId() : null;
    }

    public Integer getPartyNameId() {
        return partyNameId;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(final String complement) {
        this.complement = complement;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyNameComplement)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyNameComplement other = (ParPartyNameComplement) obj;

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

    /**
     * Řazení objektů.
     */
    public static class ParPartyNameComplementComparator implements Comparator<ParPartyNameComplement> {

        @Override
        public int compare(final ParPartyNameComplement o1, final ParPartyNameComplement o2) {
            return o1.getComplementType().getViewOrder().compareTo(o2.getComplementType().getViewOrder());
        }
    }
}
