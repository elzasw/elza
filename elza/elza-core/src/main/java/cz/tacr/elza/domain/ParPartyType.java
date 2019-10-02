package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.core.data.PartyType;


/**
 * Číselník typů osob.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "par_party_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "partyTypeEnum"})
public class ParPartyType {

    /* Konstanty pro vazby a fieldy. */
    public static final String PARTY_TYPE_ID = "partyTypeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partyTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;


    /**
     * Vlastní ID.
     * @return id
     */
    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    /**
     * Vlastní ID.
     * @param partyTypeId id
     */
    public void setPartyTypeId(final Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
    }

    /**
     * Kód typu osoby.
     * @return kód typu
     */
    public String getCode() {
        return code;
    }

    /**
     * Kód typu osoby.
     * @param code kód typu
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Název typu osoby.
     * @return název typu
     */
    public String getName() {
        return name;
    }

    /**
     * Název typu osoby.
     * @param name název typu
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Popis.
     * @return popis
     */
    public String getDescription() {
        return description;
    }

    /**
     * Popis.
     * @param description popis
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyType other = (ParPartyType) obj;

        return new EqualsBuilder().append(partyTypeId, other.getPartyTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyType pk=" + partyTypeId;
    }

    public PartyType toEnum() {
        return PartyType.fromId(partyTypeId);
    }
}
