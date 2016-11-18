package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * //TODO marik missing comment
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyTypeRelation<PPT extends ParPartyType, PRT extends ParRelationType> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getPartyTypeRelationId();

    void setPartyTypeRelationId(Integer partyTypeRelationId);

    PRT getRelationType();

    void setRelationType(PRT relationType);

    PPT getPartyType();

    void setPartyType(PPT partyType);

    boolean isRepeatable();

    void setRepeatable(boolean repeatable);

    Integer getViewOrder();

    void setViewOrder(Integer viewOrder);

    String getName();

    void setName(String name);
}
