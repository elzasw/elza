package cz.tacr.elza.cam.v2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cz.tacr.cam.v2.schema.cam.CodeXml;
import cz.tacr.cam.v2.schema.cam.DateTimeXml;
import cz.tacr.cam.v2.schema.cam.EntityIdXml;
import cz.tacr.cam.v2.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v2.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.LongStringXml;
import cz.tacr.cam.v2.schema.cam.RevisionInfoXml;
import cz.tacr.cam.v2.schema.cam.UuidXml;
import cz.tacr.elza.cam.v2.export.CamUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.GroovyService;

public class EntityXmlBuilder extends CamXmlBuilder {

    private ApState apState;

    public EntityXmlBuilder(StaticDataProvider sdp,
                            ApAccessPoint accessPoint,
                            ApState apState,
                            Collection<ApBindingState> bindingStates,
                            GroovyService groovyService,
                            DataService dataService,
                            ApScope scope,
                            boolean applyFilter) {
        super(sdp, accessPoint, bindingStates, groovyService, dataService, scope);
        this.apState = apState;
        this.applyFilter = applyFilter;
    }

    public EntityXml build(Collection<ApPart> partList, Map<Integer, List<ApItem>> itemMap,
                           Map<Integer, Collection<ApIndex>> indexMap) {

        EntityXml ent = new EntityXml();
        ent.setEntityId(new EntityIdXml(apState.getAccessPointId()));
        ent.setEntityUuid(new UuidXml(apState.getAccessPoint().getUuid()));
        // entity class
        ent.setEntityType(new CodeXml(apState.getApType().getCode()));

        // set state
        EntityRecordStateXml ens;
        if (apState.getDeleteChangeId() != null) {
            if (apState.getReplacedBy() != null) {
                ApAccessPoint replacedBy = apState.getReplacedBy();
                // set ID/UUID if available in binding
                ens = EntityRecordStateXml.ERS_REPLACED;
                EntityRecordRefXml entityRecordRef = new EntityRecordRefXml(new EntityIdXml(replacedBy.getAccessPointId()), new UuidXml(replacedBy.getUuid()));
                ent.setReplacedBy(entityRecordRef);
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
        ent.setState(ens);

        RevisionInfoXml revInfo = createRevInfo();
        ent.setRevision(revInfo);

        // Prepare empty parts
        ent.setParts(this.createParts(partList, itemMap, indexMap));

        return ent;
    }

    private RevisionInfoXml createRevInfo() {
    	RevisionInfoXml revInfo = new RevisionInfoXml();

        // Set revision id to UUID of accesspoint
        // TODO: User proper UUID of revision (when will be available)
        revInfo.setRev(new UuidXml(UUID.randomUUID().toString()));

        ApChange createChange = apState.getCreateChange();
        revInfo.setCreatedAt(new DateTimeXml(createChange.getChangeDate().toLocalDateTime()));

        // User info
        String usr = "system";
        UsrUser user = createChange.getUser();
        if (user != null) {
            // TODO: Improve user info
            usr = user.getUsername();
        }
        revInfo.setExternalUser(new LongStringXml(usr));
        return revInfo;
    }

    /**
     * Create system neutral entity reference
     * base on UUID
     */
    // TODO: allow to genereate system specific reference
    //       requires to have ApExternalSystem as parameter
	@Override
	protected EntityRecordRefXml createEntityRef(ArrDataRecordRef recordRef) {
        String uuid = null;
        ApAccessPoint ap = recordRef.getRecord();
        ApBinding binding = recordRef.getBinding();

        if (ap != null) {
            uuid = ap.getUuid();
        } else if (binding != null) {
            String bindingValue = recordRef.getBinding().getValue();
            try {
                // check if binding value is uuid
                UUID.fromString(bindingValue);
                uuid = bindingValue;
            } catch (IllegalArgumentException e) {
                // binding value is not UUID
                // reference cannot be propageted
                return null;
            }
        }

        // safety check
        if (uuid == null) {
            return null;
        }

        EntityRecordRefXml entityRecordRef = new EntityRecordRefXml();

        UuidXml uuidXml = CamUtils.getObjectFactory().createUuidXml();
        uuidXml.setValue(uuid);
        entityRecordRef.setEntityUuid(uuidXml);
        return entityRecordRef;
	}

}
