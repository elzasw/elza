package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Seznam typů vztahů.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParRelationType<CT> extends Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getRelationTypeId();

    void setRelationTypeId(Integer relationTypeId);

    String getName();

    void setName(String name);

    String getCode();

    void setCode(String code);

    CT getRelationClassType();

    void setRelationClassType(CT relationClassType);

    UseUnitdateEnum getUseUnitdate();

    void setUseUnitdate(UseUnitdateEnum useUnitdate);
}
