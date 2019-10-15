package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemTextVO extends ApItemVO {

    /**
     * text
     */
    private String value;

    public ApItemTextVO() {
    }

    public ApItemTextVO(final ApItem item) {
        super(item);
        ArrDataText data = (ArrDataText) item.getData();
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
        ArrDataText data = new ArrDataText();
        data.setValue(value);
        data.setDataType(DataType.TEXT.getEntity());
        return data;
    }
}
