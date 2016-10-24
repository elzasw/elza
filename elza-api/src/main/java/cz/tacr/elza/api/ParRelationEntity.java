package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * //TODO marik missing comment
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParRelationEntity<PR extends ParRelation, RR extends RegRecord, PRRT extends ParRelationRoleType> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getRelationEntityId();

    void setRelationEntityId(Integer relationEntityId);

    PR getRelation();

    void setRelation(PR relation);

    RR getRecord();

    void setRecord(RR record);

    PRRT getRoleType();

    void setRoleType(PRRT roleType);

    String getNote();

    void setNote(final String note);
}
