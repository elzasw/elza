package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - int.
 *
 * @since 8.1.2016
 */
public class ArrItemIntVO extends ArrItemVO {

    /**
     * celé číslo
     */
    private Integer value;

    public ArrItemIntVO() {

    }

    public ArrItemIntVO(ArrItem item, final Integer value) {
        super(item);
        this.value = value;
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
        data.setIntegerValue(value);
        data.setDataType(DataType.INT.getEntity());
        return data;
    }

    public static ArrItemIntVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        Integer value = null;
        if (data != null) {
            if (!(data instanceof ArrDataInteger)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataInteger dataInt = (ArrDataInteger) data;
            value = dataInt.getIntegerValue();
        }
        ArrItemIntVO vo = new ArrItemIntVO(item, value);
        return vo;
    }
}
