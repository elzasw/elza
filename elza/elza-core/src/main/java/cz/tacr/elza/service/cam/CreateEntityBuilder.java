package cz.tacr.elza.service.cam;

import java.util.List;
import java.util.Map;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.CreateEntityXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;

public class CreateEntityBuilder extends CamXmlBuilder {

    private ApState apState;
    final private ExternalSystemService externalSystemService;
    final private ApBinding binding;
    final ApChange change;

    public CreateEntityBuilder(final ExternalSystemService externalSystemService,
                               final StaticDataProvider sdp,
                               final ApAccessPoint accessPoint,
                               final ApBinding binding,
                               final ApState state,
                               final ApChange change,
                               final GroovyService groovyService,
                               final AccessPointDataService apDataService,
                               final ApScope scope) {
        super(sdp, accessPoint, new BindingRecordRefHandler(binding), groovyService, apDataService, scope);
        this.apState = state;
        this.externalSystemService = externalSystemService;
        this.binding = binding;
        this.change = change;
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
        externalSystemService.createApBindingItem(binding, change, uuid, null, item);
    }

    @Override
    protected void onPartCreated(ApPart apPart, String uuid) {
        externalSystemService.createApBindingItem(binding, change, uuid, apPart, null);
    }
}
