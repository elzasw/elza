package cz.tacr.elza.controller.vo.nodes.descitems;

import java.math.BigDecimal;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;


/**
 * VO hodnoty atributu - decimal.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemDecimalVO extends ArrItemVO {

    /**
     * desetinné číslo
     */
    private BigDecimal value;

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
}