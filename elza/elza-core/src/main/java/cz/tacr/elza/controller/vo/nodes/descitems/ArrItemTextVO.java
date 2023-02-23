package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - text.
 *
 * @since 8.1.2016
 */
public class ArrItemTextVO extends ArrItemVO {

    /**
     * text
     */
    private String value;

    public ArrItemTextVO() {

    }

    public ArrItemTextVO(ArrItem item, String value) {
        super(item);
        this.value = value;
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
        data.setTextValue(value.trim());
        data.setDataType(DataType.TEXT.getEntity());
        return data;
    }

    public static ArrItemTextVO newInstance(ArrItem item) {
        ArrData data = item.getData();
        String value = null;
        if (data != null) {
            if (!(data instanceof ArrDataText)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", data.getClass());
            }
            ArrDataText dataText = (ArrDataText) data;
            value = dataText.getTextValue();
        }
        ArrItemTextVO vo = new ArrItemTextVO(item, value);
        return vo;
    }
}
