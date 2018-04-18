package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.service.vo.ApAccessPointData;
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
 * Processing access point entries for access points or parties. Implementation is not thread-safe.
 */
public class AccessPointEntryProcessor implements ItemProcessor {

    protected final AccessPointsContext context;

    protected final StaticDataProvider staticData;

    protected final boolean partyRelated;

    private ApType apType;

    private ApExternalSystem externalSystem;

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
        ApAccessPointData ap = createAP(entry);
        AccessPointInfo apInfo = addAccessPoint(ap, entry.getId());
        processSubEntities(apInfo);
    }

    protected void prepareCachedReferences(AccessPointEntry item) {
        apType = staticData.getApTypeByCode(item.getT());
        if (item.getEid() != null) {
            //TODO [fric] muzu brat prvni polozku?
            externalSystem = context.getExternalSystemByCode(item.getEid().get(0).getEsc());
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

        // validate external system
        //TODO [fric] muzu brat prvni polozku?
        ExternalId eid = item.getEid().get(0);
        if (eid != null) {
            if (StringUtils.isEmpty(eid.getId())) {
                throw new DEImportException("AccessPointEntry external id is not valid, apeId:" + item.getId());
            }
            if (externalSystem == null) {
                throw new DEImportException("External system not found, apeId:" + item.getId());
            }
        }
    }

    protected ApAccessPointData createAP(AccessPointEntry item) {
        ApAccessPoint entity = new ApAccessPoint();
        ApAccessPointData pointData = new ApAccessPointData(entity);
        //TODO [fric] co s datumem aktualizace
//        entity.setLastUpdate(XmlUtils.convertXmlDate(item.getUpd()));
        entity.setApType(apType);
        entity.setScope(context.getImportScope());
        entity.setUuid(StringUtils.trimToNull(item.getUuid()));
        ApName apName = new ApName();
        apName.setName("{import_in_progress}");
        pointData.setPreferredName(apName);
        if (externalSystem != null) {
            //TODO [fric] muzu brat prvni polozku?
            ApExternalId apExternalId = new ApExternalId();
            apExternalId.setValue(item.getEid().get(0).getId());
            pointData.setExternalId(apExternalId);
            pointData.setExternalSystem(externalSystem);
        }
        return pointData;
    }

    protected AccessPointInfo addAccessPoint(ApAccessPointData ap, String entryId) {
        return context.addAccessPoint(ap, entryId);
    }

    protected void processSubEntities(AccessPointInfo apInfo) {
    }
}
