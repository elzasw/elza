package cz.tacr.elza.controller.vo.ap.item;

import java.util.Objects;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.persistence.EntityManager;

import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;

public class ApItemUnitdateVO extends ApItemVO {

    /**
     * Hodnota UnitDate
     */
    private String value;

    public ApItemUnitdateVO() {
    }

    public ApItemUnitdateVO(final AccessPointItem item) {
        super(item);
        ArrDataUnitdate data = HibernateUtils.unproxy(item.getData());
        if (data != null) {
            value = UnitDateConverter.convertToString(data);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrDataUnitdate createDataEntity(EntityManager em) {
        ArrDataUnitdate data = ArrDataUnitdate.valueOf(value);
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        String value = null;
        ArrDataUnitdate data = HibernateUtils.unproxy(item.getData());
        if (data != null) {
            value = UnitDateConverter.convertToString(data);
        }
        return equalsBase(item) && Objects.equals(this.value, value);
    }
}
