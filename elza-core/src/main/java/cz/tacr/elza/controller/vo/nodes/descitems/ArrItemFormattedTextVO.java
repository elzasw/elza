package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

/**
 * VO hodnoty atributu - formatted text.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemFormattedTextVO extends ArrItemVO {

    /**
     * formátovaných text
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
        ArrDataText data = new ArrDataText();
        data.setValue(value);
        data.setDataType(DataType.FORMATTED_TEXT.getEntity());
        return data;
    }
}
