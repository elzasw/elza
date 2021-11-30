package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

import java.math.BigDecimal;
import java.util.Objects;

import javax.persistence.EntityManager;

public class ApItemFormattedTextVO extends ApItemVO {

    /**
     * Formátovaný text
     */
    private String value;

    public ApItemFormattedTextVO() {
    }

    public ApItemFormattedTextVO(final AccessPointItem item) {
        super(item);
        value = getStringValue(item);
    }

    final public String getStringValue(final AccessPointItem item) {
        ArrDataText data = (ArrDataText) item.getData();
        return data == null ? null : data.getTextValue();
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
        data.setTextValue(value);
        data.setDataType(DataType.FORMATTED_TEXT.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(ApItem item) {
        return equalsBase(item) && Objects.equals(value, getStringValue(item));
    }
}
