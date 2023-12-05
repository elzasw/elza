package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StringNormalize;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

import java.util.Objects;

import jakarta.persistence.EntityManager;

public class ApItemTextVO extends ApItemVO {

    /**
     * Text
     */
    private String value;

    public ApItemTextVO() {
    }

    public ApItemTextVO(final AccessPointItem item) {
        super(item);
        value = getStringValue(item);
    }

    final public String getStringValue(final AccessPointItem item) {
        ArrDataText data = HibernateUtils.unproxy(item.getData());
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
        data.setTextValue(StringNormalize.normalizeText(value));
        data.setDataType(DataType.TEXT.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getStringValue(item));
    }
}
