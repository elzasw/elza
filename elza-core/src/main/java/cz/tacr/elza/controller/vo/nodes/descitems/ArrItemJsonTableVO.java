package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

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

    public ArrItemJsonTableVO() {
    	
    }
    
    public ArrItemJsonTableVO(ArrItem item, final ElzaTable value) {
        super(item);
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataJsonTable data = new ArrDataJsonTable();
        data.setValue(value);
        data.setDataType(DataType.JSON_TABLE.getEntity());
        return data;
    }

    public static ArrItemJsonTableVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        ElzaTable value = null;
        if (data != null) {
            if (!(data instanceof ArrDataJsonTable)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataJsonTable jsonTable = (ArrDataJsonTable) data;
            value = jsonTable.getValue();
        }
        ArrItemJsonTableVO vo = new ArrItemJsonTableVO(item, value);
        return vo;
    }
}
