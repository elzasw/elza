package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;

import javax.persistence.EntityManager;

public class ApItemBitVO extends ApItemVO {

    private Boolean value;

    public ApItemBitVO() {
    }

    public ApItemBitVO(final ApItem item) {
        super(item);
        ArrDataBit data = (ArrDataBit) item.getData();
        value = data == null ? null : data.isValue();
    }

    public Boolean getValue() {
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
