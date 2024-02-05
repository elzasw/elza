package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - unit id.
 *
 * @since 8.1.2016
 */
public class ArrItemUnitidVO extends ArrItemVO {

    /**
     * unikátní identifikátor
     */
    private String value;

    public ArrItemUnitidVO() {
    	
    }
    
    public ArrItemUnitidVO(ArrItem item, final String value) {
        super(item);
        this.value = value;
    }

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

    public static ArrItemUnitidVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        String value = null;
        if (data != null) {
            if (!(data instanceof ArrDataUnitid)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataUnitid unitid = (ArrDataUnitid) data;
            value = unitid.getFulltextValue();
        }
        ArrItemUnitidVO vo = new ArrItemUnitidVO(item, value);
        return vo;
    }
}
