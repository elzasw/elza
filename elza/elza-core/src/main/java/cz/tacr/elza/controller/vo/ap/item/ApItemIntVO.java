package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;

import java.util.Objects;

import jakarta.persistence.EntityManager;

public class ApItemIntVO extends ApItemVO {

    /**
     * Celé číslo
     */
    private Integer value;

    public ApItemIntVO() {
    }

    public ApItemIntVO(final AccessPointItem item) {
        super(item);
        value = getIntegerValue(item);
    }

    final public Integer getIntegerValue(final AccessPointItem item) {
        ArrDataInteger data = HibernateUtils.unproxy(item.getData());
        return data == null ? null : data.getIntegerValue();
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataInteger data = new ArrDataInteger();
        data.setIntegerValue(value);
        data.setDataType(DataType.INT.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getIntegerValue(item));
    }
}
