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

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Identifikace o přiřazených kódech původce, například IČO.
 *
 */
@Entity(name = "par_party_group_identifier")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyGroupIdentifier {

    public static final String PARTY_GROUP_FK = "partyGroup.partyId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partyGroupIdentifierId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    private ParUnitdate to;

    @RestResource(exported = false)
    @Column(insertable = false, updatable = false)
    private Integer toUnitdateId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    private ParUnitdate from;

    @RestResource(exported = false)
    @Column(insertable = false, updatable = false)
    private Integer fromUnitdateId;

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


    public Integer getPartyGroupIdentifierId() {
        return partyGroupIdentifierId;
    }

    public void setPartyGroupIdentifierId(final Integer partyGroupIdentifierId) {
        this.partyGroupIdentifierId = partyGroupIdentifierId;
    }

    public ParUnitdate getTo() {
        return to;
    }

    public void setTo(final ParUnitdate to) {
        this.to = to;
        this.toUnitdateId = to != null ? to.getUnitdateId() : null;
    }

    public Integer getToUnitdateId() {
        return toUnitdateId;
    }

    public ParUnitdate getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdate from) {
        this.from = from;
        this.fromUnitdateId = from != null ? from.getUnitdateId() : null;
    }

    public Integer getFromUnitdateId() {
        return fromUnitdateId;
    }

    public ParPartyGroup getPartyGroup() {
        return partyGroup;
    }

    public void setPartyGroup(final ParPartyGroup partyGroup) {
        this.partyGroup = partyGroup;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getIdentifier() {
        return identifier;
    }

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
