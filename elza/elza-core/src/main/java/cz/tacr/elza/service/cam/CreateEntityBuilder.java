package cz.tacr.elza.service.cam;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.CreateEntityXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityRefXml;
import cz.tacr.cam.schema.cam.PartsXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;

public class CreateEntityBuilder extends BatchUpdateBuilder {

    final private ApState apState;
    private CreateEntityXml createEntity;	

    public CreateEntityBuilder(final ExternalSystemService externalSystemService,
                               final StaticDataProvider sdp,
                               final ApAccessPoint accessPoint,
                               final ApState state,
                               final ApExternalSystem apExternalSystem,
                               final GroovyService groovyService,
                               final DataService dataService,
                               final ApScope scope) {
        super(sdp, accessPoint, groovyService, dataService, scope, apExternalSystem, externalSystemService);
        this.apState = state;
    }

    @Override
    protected EntityRefXml createBatchEntityRecordRef() {
    	EntityRefXml er = new EntityRefXml(createEntity);
    	return er;
    }

    public boolean build(List<ApPart> srcPartList,
                                 Map<Integer, List<ApItem>> itemMap) {
        Validate.isTrue(CollectionUtils.isEmpty(trgList));

        PartsXml xmlParts = createParts(srcPartList, itemMap, null);
        // Check if parts exists
        if(xmlParts==null||xmlParts.getList().size()==0) {
        	return false;
        }

        this.createEntity = new CreateEntityXml();
        createEntity.setLid("LID" + apState.getAccessPointId());
        createEntity.setEt(new CodeXml(apState.getApType().getCode()));
        createEntity.setEuid(new UuidXml(accessPoint.getUuid()));
        createEntity.setPrts(xmlParts);

        trgList.add(createEntity);
        
		// set entity state
		if(this.apState.getStateApproval()==StateApproval.APPROVED) {			
			this.setRecordState(EntityRecordStateXml.ERS_APPROVED);			
			
			bingingStates.put(apState.getAccessPointId(), EntityRecordStateXml.ERS_APPROVED.toString());
		} else {
			bingingStates.put(apState.getAccessPointId(), EntityRecordStateXml.ERS_NEW.toString());
		}

		return true;
    }
}
