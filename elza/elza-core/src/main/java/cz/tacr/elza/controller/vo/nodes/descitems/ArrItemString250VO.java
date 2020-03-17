package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.exception.SystemException;

import javax.persistence.EntityManager;

/**
 * VO hodnoty atributu - string.
 * TODO : gotzy - zamyslet se odkud dědit, kvůli chybám structureTestu
 * @author Tomáš Gotzy
 * @since 16.3.2020
 */
public class ArrItemString250VO extends ArrItemStringVO {

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
        data.setDataType(DataType.STRING_250.getEntity());
        if(value.length() > data.getDataType().getTextLengthLimit()) {
            throw new SystemException("Délka řetězce přesahuje limit: " + data.getDataType().getTextLengthLimit());
        }
        data.setValue(value);
        return data;
    }
}
