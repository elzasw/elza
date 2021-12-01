package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;

import java.util.Objects;

import javax.persistence.EntityManager;

public class ApItemBitVO extends ApItemVO {

    private Boolean value;

    public ApItemBitVO() {
    }

    public ApItemBitVO(final AccessPointItem item) {
        super(item);
        value = getBitValue(item);
    }

    final public Boolean getBitValue(final AccessPointItem item) {
        ArrDataBit data = (ArrDataBit) item.getData();
        return data == null ? null : data.isBitValue();
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
        data.setBitValue(value);
        data.setDataType(DataType.BIT.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getBitValue(item));
    }
}
