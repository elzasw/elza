package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemUnitidVO extends ApItemVO {

    /**
     * unikátní identifikátor
     */
    private String value;

    public ApItemUnitidVO() {
    }

    public ApItemUnitidVO(final ApItem item) {
        super(item);
        ArrDataUnitid data = (ArrDataUnitid) item.getData();
        value = data == null ? null : data.getValue();
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
        data.setValue(value);
        data.setDataType(DataType.UNITID.getEntity());
        return data;
    }
}
