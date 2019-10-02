package cz.tacr.elza.print.party;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ParPerson;

/**
 * Person
 */
public class Person extends Party {

    public Person(ParPerson parPerson, PartyInitHelper initHelper) {
        super(parPerson, initHelper);
    }

    @Override
    public PartyType getPartyType() {
        return PartyType.PERSON;
    }
}
