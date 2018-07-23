package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemEnumVO extends ApItemVO {

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataNull data = new ArrDataNull();
        data.setDataType(DataType.ENUM.getEntity());
        return data;
    }
}
