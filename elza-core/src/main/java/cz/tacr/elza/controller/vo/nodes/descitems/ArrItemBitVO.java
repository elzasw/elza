package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;

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
        data.setValue(value);
        data.setDataType(DataType.BIT.getEntity());
        return data;
    }
}
