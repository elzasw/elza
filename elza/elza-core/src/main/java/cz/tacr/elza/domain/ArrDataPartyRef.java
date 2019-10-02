package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu ParParty.
 */
@Entity(name = "arr_data_party_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataPartyRef extends ArrData {

    public static final String FIELD_PARTY = "party";

    @RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(name = "partyId", updatable = false, insertable = false)
    private Integer partyId;

    @Column
    private Integer position;

    private static ApFulltextProvider fulltextProvider;

	public ArrDataPartyRef() {

	}

	protected ArrDataPartyRef(ArrDataPartyRef src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataPartyRef src) {
        this.party = src.party;
        this.partyId = src.partyId;
        this.position = src.position;
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
        this.partyId = party == null ? null : party.getPartyId();
    }

    public Integer getPartyId() {
        return partyId;
    }

    @Override
    public String getFulltextValue() {
        return fulltextProvider.getFulltext(party.getAccessPoint());
    }

	@Override
	public ArrDataPartyRef makeCopy() {
		return new ArrDataPartyRef(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataPartyRef src = (ArrDataPartyRef)srcData;
        return partyId.equals(src.partyId);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataPartyRef src = (ArrDataPartyRef) srcData;
        copyValue(src);
    }

    public static void setFulltextProvider(ApFulltextProvider fullTextProvider) {
        ArrDataPartyRef.fulltextProvider = fullTextProvider;
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(party);
        Validate.notNull(partyId);
    }
}
