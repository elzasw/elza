package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Působnost osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_party_time_range")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyTimeRange implements cz.tacr.elza.api.ParPartyTimeRange<ParParty, ParUnitdate> {

    public static final String PARTY = "party";

    @Id
    @GeneratedValue
    private Integer partyTimeRangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    private ParUnitdate from;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    private ParUnitdate to;


    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(final ParParty party) {
        this.party = party;
    }

    @Override
    public Integer getPartyTimeRangeId() {
        return partyTimeRangeId;
    }

    @Override
    public void setPartyTimeRangeId(final Integer partyTimeRangeId) {
        this.partyTimeRangeId = partyTimeRangeId;
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
    public ParUnitdate getTo() {
        return to;
    }

    @Override
    public void setTo(final ParUnitdate to) {
        this.to = to;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartyTimeRange)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyTimeRange other = (ParPartyTimeRange) obj;

        return new EqualsBuilder().append(partyTimeRangeId, other.getPartyTimeRangeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyTimeRangeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyTimeRange pk=" + partyTimeRangeId;
    }
}
