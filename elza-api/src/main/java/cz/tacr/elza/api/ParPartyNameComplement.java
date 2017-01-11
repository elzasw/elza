package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Doplňky jmen osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyNameComplement<CT extends ParComplementType> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getPartyNameComplementId();

    void setPartyNameComplementId(Integer partyNameComplementId);

    CT getComplementType();

    void setComplementType(CT complementType);

    String getComplement();

    void setComplement(String complement);

}
