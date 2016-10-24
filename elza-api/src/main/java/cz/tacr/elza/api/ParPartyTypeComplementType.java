package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Vazba M:N mezi typem osoby a typem doplňku jména.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyTypeComplementType<PPT extends ParPartyType, PCT extends ParComplementType> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getPartyTypeComplementTypeId();

    void setPartyTypeComplementTypeId(Integer parPartyTypeComplementTypeId);

    PCT getComplementType();

    void setComplementType(PCT complementType);

    PPT getPartyType();

    void setPartyType(PPT partyType);

    boolean isRepeatable();

    void setRepeatable(boolean repeatable);
}
