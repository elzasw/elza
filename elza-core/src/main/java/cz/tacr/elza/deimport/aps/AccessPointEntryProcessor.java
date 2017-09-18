package cz.tacr.elza.deimport.aps;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.AccessPointsContext;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.ExternalId;
import cz.tacr.elza.utils.XmlUtils;

/**
 * Processing access point entries for access points or parties. Implementation is not thread-safe.
 */
public class AccessPointEntryProcessor implements ItemProcessor {

    protected final AccessPointsContext context;

    protected final StaticDataProvider staticData;

    protected final boolean partyRelated;

    private RegRegisterType registerType;

    private RegExternalSystem externalSystem;

    private RecordImportInfo parentInfo;

    public AccessPointEntryProcessor(ImportContext context, boolean partyRelated) {
        this.context = context.getAccessPoints();
        this.staticData = context.getStaticData();
        this.partyRelated = partyRelated;
    }

    @Override
    public void process(Object item) {
        AccessPointEntry ape = (AccessPointEntry) item;
        prepareCachedReferences(ape);
        validateAccessPointEntry(ape);
        RegRecord record = createRecord(ape);
        RecordImportInfo recordInfo = addAccessPoint(record, ape.getId());
        processSubEntities(recordInfo);
    }

    protected void prepareCachedReferences(AccessPointEntry item) {
        registerType = staticData.getRegisterTypeByCode(item.getT());
        if (item.getEid() != null) {
            externalSystem = context.getExternalSystemByCode(item.getEid().getEsc());
        }
        if (StringUtils.isNotEmpty(item.getPid())) {
            parentInfo = context.getRecordInfo(item.getPid());
        }
    }

    protected void validateAccessPointEntry(AccessPointEntry item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("AccessPointEntry id is empty");
        }
        if (item.getT() == null) {
            throw new DEImportException("AccessPointEntry type is not set, apeId:" + item.getId());
        }
        if (registerType == null) {
            throw new DEImportException("AccessPointEntry has invalid type, apeId:" + item.getId());
        }
        if (registerType.getAddRecord() == null || !registerType.getAddRecord()) {
            throw new DEImportException("AccessPointEntry type is not addable, apeId:" + item.getId());
        }
        if (partyRelated ? registerType.getPartyType() == null : registerType.getPartyType() != null) {
            throw new DEImportException(
                    "Registry type with defined party type " + (partyRelated ? "must be used" : "can be used only")
                    + " for party related AccessPointEntry, apeId:" + item.getId());
        }
        if (StringUtils.isNotEmpty(item.getPid())) {
            if (partyRelated) {
                throw new DEImportException("Party related AccessPointEntry cannot be hierarchical, apeId:" + item.getId());
            }
            if (registerType.getHierarchical() == null || !registerType.getHierarchical()) {
                throw new DEImportException("AccessPointEntry type is not hierarchical, apeId:" + item.getId());
            }
            if (parentInfo == null) {
                throw new DEImportException("AccessPointEntry parent not found, apeId:" + item.getId());
            }
            if (registerType != parentInfo.getRegisterType()) {
                throw new DEImportException("AccessPointEntry parent type does not match, apeId:" + item.getId());
            }
        }
        ExternalId eid = item.getEid();
        if (eid != null) {
            if (StringUtils.isEmpty(eid.getId())) {
                throw new DEImportException("AccessPointEntry external id is not valid, apeId:" + item.getId());
            }
            if (externalSystem == null) {
                throw new DEImportException("External system not found, apeId:" + item.getId());
            }
        }
    }

    protected RegRecord createRecord(AccessPointEntry item) {
        RegRecord record = new RegRecord();
        record.setLastUpdate(XmlUtils.convertXmlDate(item.getUpd()));
        record.setRegisterType(registerType);
        record.setScope(context.getImportScope());
        record.setUuid(StringUtils.trimToNull(item.getUuid()));
        record.setRecord("{import_in_progress}");
        if (externalSystem != null) {
            record.setExternalId(item.getEid().getId());
            record.setExternalSystem(externalSystem);
        }
        return record;
    }

    protected RecordImportInfo addAccessPoint(RegRecord record, String apeId) {
        return context.addAccessPoint(record, apeId, parentInfo);
    }

    protected void processSubEntities(RecordImportInfo recordInfo) {
    }
}
