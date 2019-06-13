package cz.tacr.elza.print.party;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ParEvent;

/**
 * Event
 */
public class Event extends Party {

    public Event(ParEvent parEvent, PartyInitHelper initHelper) {
        super(parEvent, initHelper);
    }

    @Override
    public PartyType getPartyType() {
        return PartyType.EVENT;
    }
}
