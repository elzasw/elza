package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * Hodnota atributu archivního popisu typu ParParty.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_party_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPartyRef extends ArrData {

    public static final String PARTY = "party";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(nullable = true)
    private Integer position;

    @Transient
    private final String fulltextValue;

    /**
     * Sets fulltext value index when party is only reference (detached hibernate proxy).
     */
    public ArrDataPartyRef(String fulltextValue) {
        this.fulltextValue = fulltextValue;
    }

    public ArrDataPartyRef() {
        this(null);
    }

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
    }

    @Override
    public String getFulltextValue() {
        if (fulltextValue != null) {
            return fulltextValue;
        }
        return party.getRecord().getRecord();
    }
}
