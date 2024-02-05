package cz.tacr.elza.controller.vo.nodes.descitems;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - decimal.
 *
 * @since 8.1.2016
 */
public class ArrItemDecimalVO extends ArrItemVO {

    /**
     * desetinné číslo
     */
    private BigDecimal value;

    public ArrItemDecimalVO() {
    	
    }

    public ArrItemDecimalVO(ArrItem item, final BigDecimal value) {
        super(item);
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataDecimal data = new ArrDataDecimal();
        data.setValue(value);
        data.setDataType(DataType.DECIMAL.getEntity());
        return data;
    }

    public static ArrItemDecimalVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        BigDecimal value = null;
        if (data != null) {
            if (!(data instanceof ArrDataDecimal)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataDecimal decimal = (ArrDataDecimal) data;
            value = decimal.getValue();
        }
        ArrItemDecimalVO vo = new ArrItemDecimalVO(item, value);
        return vo;
    }
}
