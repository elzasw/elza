package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Jméno abstraktní osoby.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParAbstractPartyName<AB extends ParAbstractParty> extends Versionable, Serializable {

    Integer getAbstractPartyNameId();

    void setAbstractPartyNameId(Integer abstractPartyNameId);

    AB getAbstractParty();

    void setAbstractParty(AB abstractParty);

    String getName();

    void setName(String name);
}
