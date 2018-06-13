package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApType;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.ExternalId;

/**
 * Processing access point entries for access points or parties. Implementation
 * is not thread-safe.
 * 
 * When AP storage updates persist type:
 * 1) CREATE -> all sub entities (also party) will be created
 * 2) UPDATE ->
 *      AP:
 *          - existing AP was paired by UUID or external id
 *          - storage will ignore AP entity (update not needed)
 *          - persist type in AP info will be set to UPDATE
 *          - all currently valid sub entities will be invalidate (set deleteChangeId)
 *          - all imported sub entities will be created
 *      PARTY:
 *          - party will read UPDATE type from AP info
 *          - party entity must be updated
 *          - all current sub entities must be deleted
 *          - all imported sub entities will be created
 *
 * 3) NONE -> all AP related entities (party included) will be ignored
 */
public class AccessPointEntryProcessor implements ItemProcessor {

    protected final AccessPointsContext context;

    protected final StaticDataProvider staticData;

    protected final boolean partyRelated;

    protected AccessPointInfo info;

    public AccessPointEntryProcessor(ImportContext context, boolean partyRelated) {
        this.context = context.getAccessPoints();
        this.staticData = context.getStaticData();
        this.partyRelated = partyRelated;
    }

    @Override
    public void process(Object item) {
        processEntry((AccessPointEntry) item);
    }

    protected void processEntry(AccessPointEntry entry) {
        ApAccessPoint entity = createEntity(entry);
        info = context.addAccessPoint(entity, entry.getId());
        entry.getEid().forEach(this::processExternalId);
    }

    private ApAccessPoint createEntity(AccessPointEntry entry) {
        if (StringUtils.isEmpty(entry.getId())) {
            throw new DEImportException("AccessPointEntry id is empty");
        }
        // resolve AP type
        if (entry.getT() == null) {
            throw new DEImportException("AccessPointEntry type is not set, apeId:" + entry.getId());
        }
        ApType apType = staticData.getApTypeByCode(entry.getT());
        if (apType == null) {
            throw new DEImportException("AccessPointEntry has invalid type, apeId:" + entry.getId());
        }
        if (apType.getAddRecord() == null || !apType.getAddRecord()) {
            throw new DEImportException("AccessPointEntry type is not addable, apeId:" + entry.getId());
        }
        if (partyRelated ? apType.getPartyType() == null : apType.getPartyType() != null) {
            throw new DEImportException(
                    "Registry type with defined party type " + (partyRelated ? "must be used" : "can be used only")
                            + " for party related AccessPointEntry, apeId:" + entry.getId());
        }
        // create AP
        ApAccessPoint entity = new ApAccessPoint();
        entity.setApType(apType);
        entity.setScope(context.getScope());
        entity.setUuid(StringUtils.trimToNull(entry.getUuid()));
        return entity;
    }

    private void processExternalId(ExternalId eid) {
        if (StringUtils.isEmpty(eid.getV())) {
            throw new DEImportException("External id without value, apeId=" + info.getEntryId());
        }
        ApExternalIdType apEidType = context.getEidType(eid.getT());
        if (apEidType == null) {
            throw new DEImportException("External id type not found, apEid=" + eid.getV() + ", code=" + eid.getT());
        }
        // create external id
        ApExternalId entity = new ApExternalId();
        entity.setCreateChange(context.getCreateChange());
        entity.setValue(eid.getV());
        entity.setExternalIdType(apEidType);
        context.addExternalId(entity, info);
    }
}
