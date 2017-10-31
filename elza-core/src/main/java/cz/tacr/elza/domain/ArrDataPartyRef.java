package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu ParParty.
 */
@Entity(name = "arr_data_party_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPartyRef extends ArrData {

    public static final String PARTY = "party";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(name = "partyId", updatable = false, insertable = false)
    private Integer partyId;

    @Column
    private Integer position;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public ParParty getParty() {
        return party;
    }

    public void setParty(final ParParty party) {
        this.party = party;
        this.partyId = party == null ? null : party.getPartyId();
    }

    public Integer getPartyId() {
        return partyId;
    }

    @Override
    public String getFulltextValue() {
        return party.getRecord().getRecord();
    }
}
