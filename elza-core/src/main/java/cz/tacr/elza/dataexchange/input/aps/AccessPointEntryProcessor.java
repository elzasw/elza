package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApType;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.ExternalId;

/**
 * Processing access point entries for access points or parties. Implementation is not thread-safe.
 */
public class AccessPointEntryProcessor implements ItemProcessor {

    protected final AccessPointsContext context;

    protected final StaticDataProvider staticData;

    protected final boolean partyRelated;

    private ApType apType;

    private ApExternalSystem externalSystem;

    private AccessPointInfo parentAPInfo;

    public AccessPointEntryProcessor(ImportContext context, boolean partyRelated) {
        this.context = context.getAccessPoints();
        this.staticData = context.getStaticData();
        this.partyRelated = partyRelated;
    }

    @Override
    public void process(Object item) {
        AccessPointEntry entry = (AccessPointEntry) item;
        prepareCachedReferences(entry);
        validateAccessPointEntry(entry);
        ApRecord ap = createAP(entry);
        AccessPointInfo apInfo = addAccessPoint(ap, entry.getId());
        processSubEntities(apInfo);
    }

    protected void prepareCachedReferences(AccessPointEntry item) {
        apType = staticData.getApTypeByCode(item.getT());
        if (item.getEid() != null) {
            externalSystem = context.getExternalSystemByCode(item.getEid().getEsc());
        }
        if (StringUtils.isNotEmpty(item.getPid())) {
            parentAPInfo = context.getAccessPointInfo(item.getPid());
        }
    }

    protected void validateAccessPointEntry(AccessPointEntry item) {
        if (StringUtils.isEmpty(item.getId())) {
            throw new DEImportException("AccessPointEntry id is empty");
        }
        if (item.getT() == null) {
            throw new DEImportException("AccessPointEntry type is not set, apeId:" + item.getId());
        }
        if (apType == null) {
            throw new DEImportException("AccessPointEntry has invalid type, apeId:" + item.getId());
        }
        if (apType.getAddRecord() == null || !apType.getAddRecord()) {
            throw new DEImportException("AccessPointEntry type is not addable, apeId:" + item.getId());
        }
        if (partyRelated ? apType.getPartyType() == null : apType.getPartyType() != null) {
            throw new DEImportException(
                    "Registry type with defined party type " + (partyRelated ? "must be used" : "can be used only")
                            + " for party related AccessPointEntry, apeId:" + item.getId());
        }

        // validate AP parent
        if (StringUtils.isNotEmpty(item.getPid())) {
            if (partyRelated) {
                throw new DEImportException("Party related AccessPointEntry cannot be hierarchical, apeId:" + item.getId());
            }
            if (parentAPInfo == null) {
                throw new DEImportException("AccessPointEntry parent not found, apeId:" + item.getId());
            }
            if (apType != parentAPInfo.getApType()) {
                throw new DEImportException("AccessPointEntry parent type does not match, apeId:" + item.getId());
            }
        }

        // validate external system
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

    protected ApRecord createAP(AccessPointEntry item) {
        ApRecord entity = new ApRecord();
        entity.setLastUpdate(XmlUtils.convertXmlDate(item.getUpd()));
        entity.setApType(apType);
        entity.setScope(context.getImportScope());
        entity.setUuid(StringUtils.trimToNull(item.getUuid()));
        entity.setRecord("{import_in_progress}");
        if (externalSystem != null) {
            entity.setExternalId(item.getEid().getId());
            entity.setExternalSystem(externalSystem);
        }
        return entity;
    }

    protected AccessPointInfo addAccessPoint(ApRecord ap, String entryId) {
        return context.addAccessPoint(ap, entryId, parentAPInfo);
    }

    protected void processSubEntities(AccessPointInfo apInfo) {
    }
}
