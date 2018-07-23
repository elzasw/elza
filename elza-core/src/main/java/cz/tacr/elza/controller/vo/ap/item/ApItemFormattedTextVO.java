package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemFormattedTextVO extends ApItemVO {

    /**
     * formátovaný text
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
