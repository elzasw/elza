package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.schema.v2.DescriptionItemPartyRef;

public class DescriptionItemPartyRefImpl extends DescriptionItemPartyRef {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.PARTY_REF;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        PartyImportInfo partyInfo = context.getParties().getPartyInfo(getPaid());
        if (partyInfo == null) {
            throw new DEImportException("Referenced party not found, partyId:" + getPaid());
        }
        ArrDataPartyRef data = new ArrDataPartyRef(partyInfo.getFulltext());
        data.setParty(partyInfo.getEntityRef(context.getSession(), ParParty.class));
        return data;
    }
}
