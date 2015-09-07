package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.*;


/**
 * Číselník podtypů osob.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "par_party_subtype")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartySubtype implements cz.tacr.elza.api.ParPartySubtype<ParPartyType> {

    /* Konstanty pro vazby a fieldy. */
    public static final String PARTY_TYPE = "partyType";

    @Id
    @GeneratedValue
    private Integer partySubtypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    private ParPartyType partyType;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Boolean originator;


    @Override
    public Integer getPartySubtypeId() {
        return partySubtypeId;
    }

    @Override
    public void setPartySubtypeId(final Integer partySubtypeId) {
        this.partySubtypeId = partySubtypeId;
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
    public Boolean getOriginator() {
        return originator;
    }

    @Override
    public void setOriginator(final Boolean originator) {
        this.originator = originator;
    }


    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartySubtype)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartySubtype other = (ParPartySubtype) obj;

        return new EqualsBuilder().append(partySubtypeId, other.getPartySubtypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partySubtypeId).toHashCode();
    }

}
