package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemPartyRef;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.party.Party;

public class PartyRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.PARTY_REF) {
            return null;
        }
        ArrDataPartyRef data = (ArrDataPartyRef) item.getData();
        Party party = context.getParty(data.getParty());

        return new ItemPartyRef(party);
    }

}
