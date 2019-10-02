package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.table.ElzaTable;

/**
 * VO hodnoty atributu - json table.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ArrItemJsonTableVO extends ArrItemVO {

    /**
     * celé číslo
     */
    private ElzaTable value;

    public ElzaTable getValue() {
        return value;
    }

    public void setValue(final ElzaTable value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataJsonTable data = new ArrDataJsonTable();
        data.setValue(value);
        data.setDataType(DataType.JSON_TABLE.getEntity());
        return data;
    }

}