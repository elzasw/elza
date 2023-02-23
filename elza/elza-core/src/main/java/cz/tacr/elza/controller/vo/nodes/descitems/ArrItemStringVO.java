package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - string.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemStringVO extends ArrItemVO {

    /**
     * textový řetězec
     */
    private String value;

    public ArrItemStringVO() {

    }

    public ArrItemStringVO(final ArrItem item, final String value) {
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
        ArrDataString data = new ArrDataString();
        data.setStringValue(value.trim());
        data.setDataType(DataType.STRING.getEntity());
        return data;
    }

    public static ArrItemVO newInstance(ArrItem item) {
        ArrData data = item.getData();
        String value = null;
        if (data != null) {
            if (!(data instanceof ArrDataString)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", data.getClass());
            }
            ArrDataString dataText = (ArrDataString) data;
            value = dataText.getStringValue();
        }
        ArrItemStringVO vo = new ArrItemStringVO(item, value);
        return vo;
    }
}
