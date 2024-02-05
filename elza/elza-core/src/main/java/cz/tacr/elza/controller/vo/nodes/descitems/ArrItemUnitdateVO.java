package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - unit date.
 *
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

    public ArrItemUnitdateVO() {
    	
    }

    public ArrItemUnitdateVO(ArrItem item, final String value) {
        super(item);
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataUnitdate data = ArrDataUnitdate.valueOf(value);
        return data;
    }

    public static ArrItemUnitdateVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        String value = null;
        if (data != null) {
            if (!(data instanceof ArrDataUnitdate)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) data;
            value = UnitDateConvertor.convertToString(dataUnitdate);
        }
        ArrItemUnitdateVO vo = new ArrItemUnitdateVO(item, value);
        return vo;
    }
}
