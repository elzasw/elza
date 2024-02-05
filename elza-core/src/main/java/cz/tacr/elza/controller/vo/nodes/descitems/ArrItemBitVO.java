package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import jakarta.persistence.EntityManager;

public class ArrItemBitVO extends ArrItemVO {

    private Boolean value;

    public Boolean isValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    public ArrItemBitVO() {
    	
    }

    public ArrItemBitVO(ArrItem item, final Boolean value) {
        super(item);
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataBit data = new ArrDataBit();
        data.setBitValue(value);
        data.setDataType(DataType.BIT.getEntity());
        return data;
    }

    public static ArrItemBitVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        Boolean value = null;
        if (data != null) {
            if (!(data instanceof ArrDataBit)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataBit dataBit = (ArrDataBit) data;
            value = dataBit.isBitValue();
        }
        ArrItemBitVO vo = new ArrItemBitVO(item, value);
        return vo;
    }
}
