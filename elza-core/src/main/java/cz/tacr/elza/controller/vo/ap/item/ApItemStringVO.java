package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemStringVO extends ApItemVO {

    /**
     * textový řetězec
     */
    private String value;

    public ApItemStringVO() {
    }

    public ApItemStringVO(final ApItem item) {
        super(item);
        ArrDataString data = (ArrDataString) item.getData();
        value = data == null ? null : data.getStringValue();
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataString data = new ArrDataString();
        data.setStringValue(value);
        data.setDataType(DataType.STRING.getEntity());
        return data;
    }
}
