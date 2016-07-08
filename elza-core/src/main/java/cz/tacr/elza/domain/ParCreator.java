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
 * //TODO marik missing comment
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_creator")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParCreator implements cz.tacr.elza.api.ParCreator<ParParty> {

    public static final String PARTY = "party";

    @Id
    @GeneratedValue
    private Integer creatorId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "creatorPartyId", nullable = false)
    private ParParty creatorParty;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;


    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(final ParParty party) {
        this.party = party;
    }

    @Override
    public Integer getCreatorId() {
        return creatorId;
    }

    @Override
    public void setCreatorId(final Integer creatorId) {
        this.creatorId = creatorId;
    }

    @Override
    public ParParty getCreatorParty() {
        return creatorParty;
    }

    @Override
    public void setCreatorParty(final ParParty creatorParty) {
        this.creatorParty = creatorParty;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParCreator)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParCreator other = (ParCreator) obj;

        return new EqualsBuilder().append(creatorId, other.getCreatorId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(creatorId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParCreator pk=" + creatorId;
    }
}
