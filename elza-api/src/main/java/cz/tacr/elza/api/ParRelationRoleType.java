package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Seznam rolí entit ve vztahu.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParRelationRoleType extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getRoleTypeId();

    void setRoleTypeId(Integer roleTypeId);

    String getName();

    void setName(String name);

    String getCode();

    void setCode(String code);

}
