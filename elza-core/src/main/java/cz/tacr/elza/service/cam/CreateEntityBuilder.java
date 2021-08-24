package cz.tacr.elza.service.cam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.CreateEntityXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
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

    private CreateEntityXml createEntity;

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
                               final ApState state,
                               final ApExternalSystem apExternalSystem,
                               final GroovyService groovyService,
                               final AccessPointDataService apDataService,
                               final ApScope scope) {
        super(sdp, accessPoint, new NewApRefHandler(apExternalSystem), groovyService, apDataService, scope,
                apExternalSystem.getType());
        this.apState = state;
        this.externalSystemService = externalSystemService;
    }

    public CreateEntityXml build(List<ApPart> partList,
                                 Map<Integer, List<ApItem>> itemMap) {
        Validate.isTrue(this.createEntity == null);

        CreateEntityXml createEntity = new CreateEntityXml();
        createEntity.setLid("LID" + apState.getAccessPointId());
        createEntity.setEt(new CodeXml(apState.getApType().getCode()));
        createEntity.setEuid(new UuidXml(accessPoint.getUuid()));
        createEntity.setPrts(createParts(partList, itemMap));

        this.createEntity = createEntity;
        return createEntity;
    }

    public CreateEntityXml getResult() {
        return createEntity;
    }
}
