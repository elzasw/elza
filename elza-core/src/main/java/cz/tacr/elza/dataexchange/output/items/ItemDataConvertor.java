package cz.tacr.elza.dataexchange.output.items;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.schema.v2.DescriptionItem;

public interface ItemDataConvertor {

    /**
     * @param data not-null
     * @return Converted description item.
     */
    DescriptionItem convert(ArrData data);
}
