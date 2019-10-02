package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;

import javax.persistence.EntityManager;
import java.math.BigDecimal;


/**
 * @since 18.07.2018
 */
public class ApItemDecimalVO extends ApItemVO {

    /**
     * desetinné číslo
     */
    private BigDecimal value;

    public ApItemDecimalVO() {
    }

    public ApItemDecimalVO(final ApItem item) {
        super(item);
        ArrDataDecimal data = (ArrDataDecimal) item.getData();
        value = data == null ? null : data.getValue();
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
}
