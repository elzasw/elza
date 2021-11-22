package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.DeleteItemsXml;
import cz.tacr.cam.schema.cam.DeletePartXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.NewItemsXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.SetRecordStateXml;
import cz.tacr.cam.schema.cam.UpdateEntityXml;
import cz.tacr.cam.schema.cam.UpdateItemsXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;

abstract public class BatchUpdateBuilder extends CamXmlBuilder {

	/**
     * Map of new states for access points
     */
    protected Map<Integer, String> bingingStates = new HashMap<>();
    
    protected List<Object> trgList = new ArrayList<>();
    
    final private ApExternalSystem apExternalSystem;
    
    final private ExternalSystemService externalSystemService;
    
	public Map<Integer, String> getBindingStates() {
		return bingingStates;
	}    
	
    BatchUpdateBuilder(final StaticDataProvider sdp, 
    		final ApAccessPoint accessPoint, 
    		final GroovyService groovyService, 
    		final AccessPointDataService apDataService, 
    		final ApScope scope,
    		final ApExternalSystem apExternalSystem,
    		final ExternalSystemService externalSystemService) {
		super(sdp, accessPoint, groovyService, apDataService, scope);
		this.apExternalSystem = apExternalSystem;
		this.externalSystemService = externalSystemService;
	}
    
    /**
     * Add change
     * @param batchEntityRef
     * @param change
     */
    protected void addChange(Object change) {
    	Object entityRef = createBatchEntityRecordRef();
        UpdateEntityXml result = new UpdateEntityXml(entityRef, change);
        trgList.add(result);

    }

    protected void addUpdate(NewItemsXml change) {
        addChange(change);
    }

    protected void addUpdate(PartXml change) {
        addChange(change);
    }

    protected void addUpdate(DeletePartXml change) {
        addChange(change);
    }

    protected void addUpdate(SetRecordStateXml change) {
        addChange(change);
    }

    protected void addUpdate(DeleteItemsXml change) {
        addChange(change);
    }

    protected void addUpdate(UpdateItemsXml change) {
        addChange(change);
    }

    protected void setPrefName(UuidXml prefName) {
        addChange(prefName);
    }
    
    protected void setRecordState(EntityRecordStateXml recordState) {
    	SetRecordStateXml srs = new SetRecordStateXml(recordState, null);
    	addChange(srs);
    }

    /**
	 * Store changes to the final batch update
	 * @param batchUpdate
	 */
	public void storeChanges(BatchUpdateXml batchUpdate) {		
        batchUpdate.getChanges().addAll(trgList);		
	}
	
	@Override
    protected EntityRecordRefXml createEntityRef(ArrDataRecordRef recordRef) {
    	// read binding
    	ApAccessPoint ap = recordRef.getRecord();
    	ApBinding binding = null;
    	if(ap!=null) {
    		// 
    		ApBindingState bindingState = this.externalSystemService.findByAccessPointAndExternalSystem(ap, apExternalSystem);
    		if(bindingState!=null) {
    			binding = bindingState.getBinding(); 
    		}
    	} else {   	
    		// create record ref only for records with same binding
    		if ( recordRef.getBinding() != null) {
    			ApExternalSystem bindedExtSystem = recordRef.getBinding().getApExternalSystem();
    			if (bindedExtSystem.getExternalSystemId().equals(apExternalSystem.getExternalSystemId())) {
    				binding = recordRef.getBinding();
    			}
    		}            
        }
    	
    	if(binding == null) {
    		return null;
    	}

        EntityRecordRefXml entityRecordRef = new EntityRecordRefXml();
        long entityId = Long.parseLong(binding.getValue());
        entityRecordRef.setEid(new EntityIdXml(entityId));
        return entityRecordRef;
    }
    
	/**
	 * Create entity reference
	 * @return Return reference for UpdateEntity
	 */
	abstract protected Object createBatchEntityRecordRef();
}
