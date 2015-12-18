package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * //TODO marik missing comment
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParRelationTypeRoleType<PRT extends ParRelationType, PRRT extends ParRelationRoleType> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getRelationTypeRoleTypeId();

    void setRelationTypeRoleTypeId(Integer relationTypeRoleTypeId);

    PRT getRelationType();

    void setRelationType(PRT relationType);

    PRRT getRoleType();

    void setRoleType(PRRT roleType);

}
