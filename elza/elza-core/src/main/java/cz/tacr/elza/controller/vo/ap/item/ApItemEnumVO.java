package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataString;

import javax.persistence.EntityManager;

public class ApItemEnumVO extends ApItemVO {

    public ApItemEnumVO() {
    }

    public ApItemEnumVO(final ApItem item) {
        super(item);
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataNull data = new ArrDataNull();
        data.setDataType(DataType.ENUM.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(ApItem apItem) {
        return equalsBase(apItem); 
    }
}
