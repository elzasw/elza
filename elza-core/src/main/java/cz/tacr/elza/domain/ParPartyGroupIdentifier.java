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

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Identifikace o přiřazených kódech původce, například IČO.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_group_identifier")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyGroupIdentifier implements cz.tacr.elza.api.ParPartyGroupIdentifier<ParUnitdate, ParPartyGroup> {

    @Id
    @GeneratedValue
    private Integer partyGroupIdentifierId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    private ParUnitdate to;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    private ParUnitdate from;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyGroup.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParPartyGroup partyGroup;

    @Column(length = StringLength.LENGTH_50)
    private String source;

    @Column()
    private String note;

    @Column(length = StringLength.LENGTH_50)
    private String identifier;


    @Override
    public Integer getPartyGroupIdentifierId() {
        return partyGroupIdentifierId;
    }

    @Override
    public void setPartyGroupIdentifierId(final Integer partyGroupIdentifierId) {
        this.partyGroupIdentifierId = partyGroupIdentifierId;
    }

    @Override
    public ParUnitdate getTo() {
        return to;
    }

    @Override
    public void setTo(final ParUnitdate to) {
        this.to = to;
    }

    @Override
    public ParUnitdate getFrom() {
        return from;
    }

    @Override
    public void setFrom(final ParUnitdate from) {
        this.from = from;
    }

    @Override
    public ParPartyGroup getPartyGroup() {
        return partyGroup;
    }

    @Override
    public void setPartyGroup(final ParPartyGroup partyGroup) {
        this.partyGroup = partyGroup;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(final String source) {
        this.source = source;
    }

    @Override
    public String getNote() {
        return note;
    }

    @Override
    public void setNote(final String note) {
        this.note = note;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyGroupIdentifier)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyGroupIdentifier other = (ParPartyGroupIdentifier) obj;

        return new EqualsBuilder().append(partyGroupIdentifierId, other.getPartyGroupIdentifierId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyGroupIdentifierId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyGroupIdentifier pk=" + partyGroupIdentifierId;
    }
}
