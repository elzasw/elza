package cz.tacr.elza.api;

import java.io.Serializable;

public interface RulArrangementType extends Serializable {
    
    Integer getArrangementTypeId();

    void setArrangementTypeId(Integer arrangementTypeId);

    String getName();

    void setName(String name);

    String getCode();

    void setCode(String code);
}
