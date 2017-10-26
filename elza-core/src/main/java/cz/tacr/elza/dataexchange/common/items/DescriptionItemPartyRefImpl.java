package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.schema.v2.DescriptionItemPartyRef;

public class DescriptionItemPartyRefImpl extends DescriptionItemPartyRef {

    @Override
    public ArrData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.PARTY_REF) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        PartyInfo partyInfo = context.getParties().getPartyInfo(getPaid());
        if (partyInfo == null) {
            throw new DEImportException("Referenced party not found, partyId:" + getPaid());
        }
        ArrDataPartyRef data = new ArrDataPartyRef(partyInfo.getFulltext());
        data.setParty(partyInfo.getEntityReference(context.getSession()));
        return data;
    }
}
