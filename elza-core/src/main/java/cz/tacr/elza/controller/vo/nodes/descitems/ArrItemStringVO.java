package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;

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

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataString data = new ArrDataString();
        data.setValue(value);
        data.setDataType(DataType.STRING.getEntity());
        return data;
    }
}
