package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;

public class DescriptionItemAPRefImpl extends DescriptionItemAPRef {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.RECORD_REF;
    }

    @Override
    protected ArrData createData(ImportContext context, RuleSystemItemType itemType) {
        RecordImportInfo recordInfo = context.getAccessPoints().getRecordInfo(getApid());
        if (recordInfo == null) {
            throw new DEImportException("Referenced access point not found, apeId:" + getApid());
        }
        ArrDataRecordRef data = new ArrDataRecordRef(recordInfo.getFulltext());
        data.setRecord(recordInfo.getEntityRef(context.getSession(), RegRecord.class));
        return data;
    }
}
