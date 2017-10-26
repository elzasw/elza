package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemPartyRefImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;

public class PartyRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemPartyRefImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataPartyRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
        DescriptionItemPartyRefImpl item = new DescriptionItemPartyRefImpl();
        item.setPaid(partyRef.getPartyId().toString());
        return item;
    }
}
