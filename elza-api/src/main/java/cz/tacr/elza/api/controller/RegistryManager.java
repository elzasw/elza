package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.RegRecord;


/**
 * Rozhraní pro rejstřík.
 */
public interface RegistryManager<RR extends RegRecord> {

    RR createRecord(RR regRecord);

}
