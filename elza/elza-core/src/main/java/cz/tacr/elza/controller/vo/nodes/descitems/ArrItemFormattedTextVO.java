package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - formatted text.
 *
 * @since 8.1.2016
 */
public class ArrItemFormattedTextVO extends ArrItemVO {

    /**
     * formátovaných text
     */
    private String value;

    public ArrItemFormattedTextVO() {

    }

    public ArrItemFormattedTextVO(ArrItem item, String value) {
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
        data.setTextValue(value);
        data.setDataType(DataType.FORMATTED_TEXT.getEntity());
        return data;
    }

    public static ArrItemFormattedTextVO newInstance(ArrItem item) {
        ArrData data = item.getData();
        String value = null;
        if (data != null) {
            if (!(data instanceof ArrDataText)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataText dataText = (ArrDataText) data;
            value = dataText.getTextValue();
        }
        ArrItemFormattedTextVO vo = new ArrItemFormattedTextVO(item, value);
        return vo;
    }
}
