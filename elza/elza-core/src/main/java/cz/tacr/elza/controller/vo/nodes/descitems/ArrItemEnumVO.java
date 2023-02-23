package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - enum.
 *
 * Nemá žádné parametry, protože enum je myšlený přes specifikace.
 *
 * @since 8.1.2016
 */
public class ArrItemEnumVO extends ArrItemVO {

    public ArrItemEnumVO() {

    }

    public ArrItemEnumVO(ArrItem item) {
        super(item);
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataNull data = new ArrDataNull();
        data.setDataType(DataType.ENUM.getEntity());
        return data;
    }

    public static ArrItemEnumVO newInstance(ArrItem item) {
        ArrData data = item.getData();
        if (data != null) {
            if (!(data instanceof ArrDataNull)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
        }
        ArrItemEnumVO vo = new ArrItemEnumVO(item);
        return vo;
    }
}
