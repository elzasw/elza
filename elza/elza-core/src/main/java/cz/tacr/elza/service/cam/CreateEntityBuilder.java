package cz.tacr.elza.service.cam;

import java.util.List;
import java.util.Map;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.CreateEntityXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.cam.CamXmlFactory.EntityRefHandler;

public class CreateEntityBuilder extends CamXmlBuilder {

    final private ApState apState;
    final private ExternalSystemService externalSystemService;
    final ApChange change;
    final private ApBindingState bindingState;

    private static class NewApRefHandler implements EntityRefHandler {

        private final ApExternalSystem apExternalSystem;

        public NewApRefHandler(final ApExternalSystem apExternalSystem) {
            this.apExternalSystem = apExternalSystem;
        }

        @Override
        public EntityRecordRefXml createEntityRef(ArrDataRecordRef recordRef) {
            // create record ref only for records with same binding
            ApBinding binding = recordRef.getBinding();
            if ( binding == null) {
                return null;
            }
            ApExternalSystem bindedExtSystem = binding.getApExternalSystem();
            if (!bindedExtSystem.getExternalSystemId().equals(apExternalSystem.getExternalSystemId())) {
                return null;
            }

            EntityRecordRefXml entityRecordRef = new EntityRecordRefXml();
            long entityId = Long.parseLong(binding.getValue());
            entityRecordRef.setEid(new EntityIdXml(entityId));
            return entityRecordRef;
        }

    };

    public CreateEntityBuilder(final ExternalSystemService externalSystemService,
                               final StaticDataProvider sdp,
                               final ApAccessPoint accessPoint,
                               final ApBindingState bindingState,
                               final ApState state,
                               final ApExternalSystem apExternalSystem,
                               final GroovyService groovyService,
                               final AccessPointDataService apDataService,
                               final ApScope scope) {
        super(sdp, accessPoint, new NewApRefHandler(apExternalSystem), groovyService, apDataService, scope);
        this.bindingState = bindingState;
        this.apState = state;
        this.externalSystemService = externalSystemService;
        this.change = bindingState.getCreateChange();
    }

    public CreateEntityXml build(List<ApPart> partList,
                                 Map<Integer, List<ApItem>> itemMap,
                                 String externalSystemTypeCode) {
        CreateEntityXml createEntity = new CreateEntityXml();
        createEntity.setLid("LID" + apState.getAccessPointId());
        createEntity.setEt(new CodeXml(apState.getApType().getCode()));
        createEntity.setEuid(new UuidXml(accessPoint.getUuid()));
        createEntity.setPrts(createParts(partList, itemMap, externalSystemTypeCode));
        return createEntity;
    }

    @Override
    protected void onItemCreated(ApItem item, String uuid) {
        externalSystemService.createApBindingItem(bindingState.getBinding(), change, uuid, null, item);
    }

    @Override
    protected void onPartCreated(ApPart apPart, String uuid) {
        externalSystemService.createApBindingItem(bindingState.getBinding(), change, uuid, apPart, null);
    }
}
