package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.schema.v2.DescriptionItemPartyRef;

public class DescriptionItemPartyRefImpl extends DescriptionItemPartyRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        AccessPointInfo apInfo = context.getAccessPoints().getApInfo(getPaid());
        ArrDataRecordRef data = new ArrDataRecordRef();
        data.setRecord(apInfo.getEntityRef(context.getSession()));
        data.setDataType(dataType.getEntity());
        return new ImportableItemData(data, apInfo.getFulltext());
    }
}
