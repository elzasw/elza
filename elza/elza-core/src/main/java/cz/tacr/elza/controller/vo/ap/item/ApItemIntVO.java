package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemIntVO extends ApItemVO {

    /**
     * celé číslo
     */
    private Integer value;

    public ApItemIntVO() {
    }

    public ApItemIntVO(final ApItem item) {
        super(item);
        ArrDataInteger data = (ArrDataInteger) item.getData();
        value = data == null ? null : data.getValue();
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
        data.setValue(value);
        data.setDataType(DataType.INT.getEntity());
        return data;
    }
}
