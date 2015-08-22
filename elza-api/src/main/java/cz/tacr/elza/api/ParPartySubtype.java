package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník podtypů osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartySubtype<PT extends ParPartyType> extends Serializable {

    Integer getPartySubtypeId();

    void setPartySubtypeId(Integer partySubtypeId);

    PT getPartyType();

    void setPartyType(PT partyType);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    Boolean getOriginator();

    void setOriginator(Boolean originator);
}
