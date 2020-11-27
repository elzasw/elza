package cz.tacr.elza.service.cam;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.DateTimeXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.RevInfoXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.GroovyService;

public class EntityXmlBuilder extends CamXmlBuilder {

    private ApState apState;

    public EntityXmlBuilder(StaticDataProvider sdp,
                            ApAccessPoint accessPoint,
                            ApState apState,
                            GroovyService groovyService,
                            AccessPointDataService apDataService,
                            ApScope scope) {
        super(sdp, accessPoint, new ApUuidRecordRefHandler(), groovyService, apDataService, scope);
        this.apState = apState;
    }

    public EntityXml build(Collection<ApPart> partList, Map<Integer, List<ApItem>> itemMap, String externalSystemTypeCode) {

        EntityXml ent = new EntityXml();
        ent.setEid(new EntityIdXml(apState.getAccessPointId()));
        ent.setEuid(new UuidXml(apState.getAccessPoint().getUuid()));
        // entity class
        ent.setEnt(new CodeXml(apState.getApType().getCode()));

        // set state
        EntityRecordStateXml ens;
        if (apState.getDeleteChangeId() != null) {
            if (apState.getReplacedBy() != null) {
                // TODO: set ID/UUID if available in binding
                ens = EntityRecordStateXml.ERS_REPLACED;
                ent.setReud(new UuidXml(apState.getReplacedBy().getUuid()));
            } else {
                ens = EntityRecordStateXml.ERS_INVALID;
            }
        } else {
            switch (apState.getStateApproval()) {
            case NEW:
            case TO_AMEND:
            case TO_APPROVE:
                ens = EntityRecordStateXml.ERS_NEW;
                break;
            case APPROVED:
                ens = EntityRecordStateXml.ERS_APPROVED;
                break;
            default:
                throw new SystemException("Missing mapping of internal state to CAM state");
            }
        }
        ent.setEns(ens);

        RevInfoXml revInfo = createRevInfo();
        ent.setRevi(revInfo);

        // Prepare empty parts
        ent.setPrts(this.createParts(partList, itemMap, externalSystemTypeCode));

        return ent;
    }

    private RevInfoXml createRevInfo() {
        RevInfoXml revInfo = CamXmlFactory.getObjectFactory().createRevInfoXml();

        // Set revision id to UUID of accesspoint
        // TODO: User proper UUID of revision (when will be available)
        revInfo.setRid(new UuidXml(UUID.randomUUID().toString()));

        ApChange createChange = apState.getCreateChange();
        revInfo.setModt(new DateTimeXml(createChange.getChangeDate().toLocalDateTime()));

        // User info
        String usr = "system";
        UsrUser user = createChange.getUser();
        if (user != null) {
            // TODO: Improve user info
            usr = user.getUsername();
        }
        revInfo.setUsr(new LongStringXml(usr));
        return revInfo;
    }

}
