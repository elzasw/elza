package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParParty<RR extends RegRecord, PS extends ParPartySubtype> extends Versionable, Serializable {

    Integer getAbstractPartyId();

    void setAbstractPartyId(Integer abstractPartyId);

    RR getRecord();

    void setRecord(RR record);

    PS getPartySubtype();

    void setPartySubtype(PS partySubtype);
}
