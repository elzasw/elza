package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApVariantRecord;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPointVariantNames;

/**
 * Processing access points. Implementation is not thread-safe.
 */
public class AccessPointProcessor extends AccessPointEntryProcessor {

    private AccessPoint accessPoint;

    public AccessPointProcessor(ImportContext context) {
        super(context, false);
    }

    @Override
    public void process(Object item) {
        accessPoint = (AccessPoint) item;
        super.process(accessPoint.getApe());
    }

    @Override
    protected void validateAccessPointEntry(AccessPointEntry item) {
        super.validateAccessPointEntry(item);
        if (StringUtils.isBlank(accessPoint.getN())) {
            throw new DEImportException("AccessPoint name is not set, apeId:" + item.getId());
        }
    }

    @Override
    protected ApRecord createAP(AccessPointEntry item) {
        ApRecord record = super.createAP(item);
        record.setRecord(accessPoint.getN());
        record.setCharacteristics(accessPoint.getChr());
        return record;
    }

    @Override
    protected AccessPointInfo addAccessPoint(ApRecord ap, String entryId) {
        AccessPointInfo info = super.addAccessPoint(ap, entryId);
        info.setName(accessPoint.getN());
        return info;
    }

    @Override
    protected void processSubEntities(AccessPointInfo apInfo) {
        super.processSubEntities(apInfo);
        processVariantNames(apInfo);
    }

    private void processVariantNames(AccessPointInfo apInfo) {
        AccessPointVariantNames variantNames = accessPoint.getVnms();
        if (variantNames == null) {
            return;
        }
        for (String vn : variantNames.getVnm()) {
            ApVariantRecord variantRecord = new ApVariantRecord();
            variantRecord.setRecord(vn);
            context.addVariantName(variantRecord, apInfo);
        }
    }
}
