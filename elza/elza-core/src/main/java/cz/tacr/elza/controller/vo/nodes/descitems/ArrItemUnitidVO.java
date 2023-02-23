package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;

/**
 * VO hodnoty atributu - unit id.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemUnitidVO extends ArrItemVO {

    /**
     * unikátní identifikátor
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
        ArrDataUnitid data = new ArrDataUnitid();
        data.setUnitId(value.trim());
        data.setDataType(DataType.UNITID.getEntity());
        return data;
    }
}
