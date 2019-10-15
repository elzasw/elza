package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;

public class DescriptionItemAPRefImpl extends DescriptionItemAPRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.RECORD_REF) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        AccessPointInfo apInfo = context.getAccessPoints().getApInfo(getApid());
        if (apInfo == null) {
            throw new DEImportException("Referenced access point not found, apeId:" + getApid());
        }
        ArrDataRecordRef data = new ArrDataRecordRef();
        data.setRecord(apInfo.getEntityRef(context.getSession()));
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data, apInfo.getFulltext());
    }
}
