package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.table.ElzaTable;

import javax.persistence.EntityManager;

/**
 * VO hodnoty atributu - json table.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ApItemJsonTableVO extends ApItemVO {

    /**
     * celé číslo
     */
    private ElzaTable value;

    public ApItemJsonTableVO() {
    }

    public ApItemJsonTableVO(final ApItem item) {
        super(item);
        ArrDataJsonTable data = (ArrDataJsonTable) item.getData();
        value = data == null ? null : data.getValue();
    }

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
