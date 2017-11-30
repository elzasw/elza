package cz.tacr.elza.print.party;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ParPerson;

/**
 * Person
 */
public class Person extends Party {

    private Person(ParPerson parPerson, PartyInitHelper initHelper) {
        super(parPerson, initHelper);
    }

    public static Person newInstance(ParPerson parPerson, PartyInitHelper initHelper) {
        Person person = new Person(parPerson, initHelper);
        return person;
    }

    @Override
    public String getType() {
        return PartyType.PERSON.getName();
    }

    @Override
    public String getTypeCode() {
        return PartyType.PERSON.getCode();
    }
}
