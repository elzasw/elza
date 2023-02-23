package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;

import jakarta.persistence.EntityManager;

public class ArrItemBitVO extends ArrItemVO {

    private Boolean value;

    public Boolean isValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataBit data = new ArrDataBit();
        data.setBitValue(value);
        data.setDataType(DataType.BIT.getEntity());
        return data;
    }
}
