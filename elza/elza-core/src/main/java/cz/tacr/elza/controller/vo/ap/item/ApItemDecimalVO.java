package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataDecimal;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Objects;

public class ApItemDecimalVO extends ApItemVO {

    /**
     * Desetinné číslo
     */
    private BigDecimal value;

    public ApItemDecimalVO() {
    }

    public ApItemDecimalVO(final AccessPointItem item) {
        super(item);
        value = getBigDecimalValue(item);
    }

    final public BigDecimal getBigDecimalValue(final AccessPointItem item) {
        ArrDataDecimal data = (ArrDataDecimal) item.getData();
        return data == null ? null : data.getValue();
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

    @Override
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getBigDecimalValue(item));
    }
}
