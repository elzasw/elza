package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;

/**
 * VO hodnoty atributu - unit date.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemUnitdateVO extends ArrItemVO {

    /**
     * hodnota
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataUnitdate data = ArrDataUnitdate.valueOf(value);
        return data;
    }
}
