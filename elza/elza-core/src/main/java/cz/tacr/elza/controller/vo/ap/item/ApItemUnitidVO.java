package cz.tacr.elza.controller.vo.ap.item;

import java.util.Objects;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;

public class ApItemUnitidVO extends ApItemVO {

    /**
     * Unikátní identifikátor
     */
    private String value;

    public ApItemUnitidVO() {
    }

    public ApItemUnitidVO(final AccessPointItem item) {
        super(item);
        value = getStringValue(item);
    }

    final public String getStringValue(final AccessPointItem item) {
        ArrDataUnitid data = (ArrDataUnitid) item.getData();
        return data == null ? null : data.getUnitId();
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataUnitid data = new ArrDataUnitid();
        data.setUnitId(value);
        data.setDataType(DataType.UNITID.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(ApItem item) {
        return equalsBase(item) && Objects.equals(value, getStringValue(item));
    }
}
