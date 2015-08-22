package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník typů osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyType extends Serializable {

    Integer getPartyTypeId();

    void setPartyTypeId(Integer partyTypeId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);
}
