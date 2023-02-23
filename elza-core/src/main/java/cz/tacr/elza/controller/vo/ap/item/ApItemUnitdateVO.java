package cz.tacr.elza.controller.vo.ap.item;

import java.util.Objects;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

public class ApItemUnitdateVO extends ApItemVO {

    /**
     * Hodnota UnitDate
     */
    private String value;

    public ApItemUnitdateVO() {
    }

    public ApItemUnitdateVO(final AccessPointItem item) {
        super(item);
        ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
        if (data != null) {
            value = UnitDateConvertor.convertToString(data);
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataUnitdate data = ArrDataUnitdate.valueOf(value);
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        String value = null;
        ArrDataUnitdate data = (ArrDataUnitdate) item.getData();
        if (data != null) {
            value = UnitDateConvertor.convertToString(data);
        }
        return equalsBase(item) && Objects.equals(this.value, value);
    }
}
