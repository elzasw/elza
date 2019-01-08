package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.schema.v2.DescriptionItemPartyRef;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class PartyRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemPartyRef convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataPartyRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
        DescriptionItemPartyRef item = objectFactory.createDescriptionItemPartyRef();
        item.setPaid(partyRef.getPartyId().toString());
        return item;
    }
}
