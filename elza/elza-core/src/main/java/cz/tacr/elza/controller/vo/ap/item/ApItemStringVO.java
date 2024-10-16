package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StringNormalize;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;

import java.util.Objects;

import jakarta.persistence.EntityManager;

public class ApItemStringVO extends ApItemVO {

    /**
     * Textový řetězec
     */
    private String value;

    public ApItemStringVO() {
    }

    public ApItemStringVO(final AccessPointItem item) {
        super(item);
        value = getStringValue(item);
    }

    final public String getStringValue(final AccessPointItem item) {
        ArrDataString data = HibernateUtils.unproxy(item.getData());
        return data == null ? null : data.getStringValue();
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
        data.setStringValue(StringNormalize.normalizeString(value));
        data.setDataType(DataType.STRING.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getStringValue(item));
    }
}
