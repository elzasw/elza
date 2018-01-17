package cz.tacr.elza.controller.vo.nodes.descitems;


import javax.persistence.EntityManager;

import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - structure data.
 *
 * @since 16.11.2017
 */
public class ArrItemStructureVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrStructureDataVO structureData;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ArrStructureDataVO getStructureData() {
        return structureData;
    }

    public void setStructureData(final ArrStructureDataVO structureData) {
        this.structureData = structureData;
    }

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataStructureRef data = new ArrDataStructureRef();

        if (structureData != null) {
            throw new BusinessException("Inconsistent data, structureData is not null", BaseCode.PROPERTY_IS_INVALID);
        }

        // get reference
        ArrStructureData structObj = null;
        if (this.value != null) {
            structObj = em.getReference(ArrStructureData.class, value);
        }
        data.setStructureData(structObj);

        data.setDataType(DataType.STRUCTURED.getEntity());
        return data;
    }
}
