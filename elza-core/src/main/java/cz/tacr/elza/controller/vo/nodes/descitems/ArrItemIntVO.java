package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;

/**
 * VO hodnoty atributu - int.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemIntVO extends ArrItemVO {

    /**
     * celé číslo
     */
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataInteger data = new ArrDataInteger();
        data.setValue(value);
        data.setDataType(DataType.INT.getEntity());
        return data;
    }
}