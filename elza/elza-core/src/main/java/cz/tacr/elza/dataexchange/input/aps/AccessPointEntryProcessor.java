package cz.tacr.elza.dataexchange.input.aps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.ExternalId;

/**
 * Processing access point entries for access points or parties. Implementation
 * is not thread-safe.
 *
 * When AP storage updates persist type: <br>
 * 1) CREATE -> all sub entities (also party) will be created <br>
 * 2) UPDATE -> <br>
 * AP: <br>
 * - existing AP was paired by UUID or external id <br>
 * - storage will ignore AP entity (update not needed) <br>
 * - persist type in AP info will be set to UPDATE <br>
 * - all sub entities will be invalidate (set deleteChangeId) <br>
 * - all imported sub entities will be created <br>
 * PARTY: <br>
 * - party will read UPDATE type from AP info <br>
 * - party entity must be updated <br>
 * - all current sub entities must be deleted <br>
 * - all imported sub entities will be created <br>
 * 3) NONE -> all AP related entities (party included) will be ignored
 */
public class AccessPointEntryProcessor implements ItemProcessor {

    protected final AccessPointsContext context;

    protected final StaticDataProvider staticData;

    protected final boolean partyRelated;

    protected String entryId;

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
        entryId = entry.getId();
        // create AP and prepare AP info
        ApEntity entity = createEntity(entry);
        List<ApExternalId> eids = createExternalIds(entry.getEid());
        info = context.addAccessPoint(entity.accessPoint, entry.getId(), entity.state, eids);
    }

    private List<ApExternalId> createExternalIds(Collection<ExternalId> eids) {
        if (eids.isEmpty()) {
            return null;
        }
        List<ApExternalId> entities = new ArrayList<>(eids.size());
        for (ExternalId eid : eids) {
            if (StringUtils.isEmpty(eid.getT())) {
                throw new DEImportException("External id type is not set, apeId=" + entryId);
            }
            if (StringUtils.isEmpty(eid.getV())) {
                throw new DEImportException("External id without value, apeId=" + entryId);
            }
            ApExternalIdType eidType = context.getEidType(eid.getT());
            if (eidType == null) {
                throw new DEImportException("External id type not found, apEid=" + eid.getV() + ", code=" + eid.getT());
            }
            // create external id
            ApExternalId entity = new ApExternalId();
            entity.setCreateChange(context.getCreateChange());
            entity.setValue(eid.getV());
            entity.setExternalIdType(eidType);
            entities.add(entity);
        }
        return entities;
    }

    private class ApEntity {

        ApAccessPoint accessPoint;
        ApState state;

        ApEntity(final ApAccessPoint accessPoint, final ApState state) {
            this.accessPoint = accessPoint;
            this.state = state;
        }
    }

    private ApEntity createEntity(AccessPointEntry entry) {
        if (StringUtils.isEmpty(entry.getId())) {
            throw new DEImportException("AP entry id is empty");
        }
        // resolve AP type
        if (entry.getT() == null) {
            throw new DEImportException("AP type is not set, apeId:" + entry.getId());
        }
        ApType apType = staticData.getApTypeByCode(entry.getT());
        if (apType == null) {
            throw new DEImportException("AP has invalid type, apeId:" + entry.getId());
        }
        if (apType.isReadOnly()) {
            throw new DEImportException("AP type is read only, apeId:" + entry.getId());
        }

        // create AP
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setUuid(StringUtils.trimToNull(entry.getUuid()));
        accessPoint.setState(ApStateEnum.OK);

        ApState apState = new ApState();
        apState.setAccessPoint(accessPoint);
        apState.setApType(apType);
        apState.setScope(context.getScope());
        apState.setStateApproval(ApState.StateApproval.APPROVED);
        apState.setCreateChange(context.getCreateChange());

        return new ApEntity(accessPoint, apState);
    }
}
