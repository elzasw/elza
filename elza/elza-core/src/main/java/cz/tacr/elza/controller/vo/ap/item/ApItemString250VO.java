package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemString250VO extends ApItemVO {

    /**
     * textový řetězec
     */
    private String value;

    public ApItemString250VO() {
    }

    public ApItemString250VO(final ApItem item) {
        super(item);
        ArrDataString data = (ArrDataString) item.getData();
        if(data.getValue().length() > 250) {
            throw new SystemException("Délka řetězce přesahuje limit: " + item.getItemType().getDataType().getTextLengthLimit(), BaseCode.PROPERTY_IS_INVALID)
                    .set("item", item.getItemId());
        }
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
        ArrDataString data = new ArrDataString();
        data.setValue(value);
        data.setDataType(DataType.STRING_250.getEntity());
        return data;
    }
}
