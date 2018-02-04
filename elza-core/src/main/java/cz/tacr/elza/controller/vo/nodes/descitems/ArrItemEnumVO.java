package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataNull;

/**
 * VO hodnoty atributu - enum.
 *
 * Nemá žádné parametry, protože enum je myšlený přes specifikace.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemEnumVO extends ArrItemVO {


    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataNull data = new ArrDataNull();
        data.setDataType(DataType.ENUM.getEntity());
        return data;
    }
}