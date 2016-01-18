package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;


/**
 * Číselník typů osob.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "par_party_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "partyTypeEnum"})
public class ParPartyType implements cz.tacr.elza.api.ParPartyType {

    /* Konstanty pro vazby a fieldy. */
    public static final String PARTY_TYPE_ID = "partyTypeId";

    @Id
    @GeneratedValue
    private Integer partyTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;


    @Override
    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    @Override
    public void setPartyTypeId(final Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
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

    public PartyTypeEnum getPartyTypeEnum() {
        return PartyTypeEnum.valueOf(code);
    }

    public enum PartyTypeEnum {
        PERSON,
        DYNASTY,
        GROUP_PARTY,
        EVENT
    }

}
