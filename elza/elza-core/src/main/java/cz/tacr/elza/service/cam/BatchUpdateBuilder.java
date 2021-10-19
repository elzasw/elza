package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.DeleteItemsXml;
import cz.tacr.cam.schema.cam.DeletePartXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.NewItemsXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.SetRecordStateXml;
import cz.tacr.cam.schema.cam.UpdateEntityXml;
import cz.tacr.cam.schema.cam.UpdateItemsXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.cam.CamXmlFactory.EntityRefHandler;

abstract public class BatchUpdateBuilder extends CamXmlBuilder {

	/**
     * Map of new states for access points
     */
    protected Map<Integer, String> bingingStates = new HashMap<>();
    
    protected List<Object> trgList = new ArrayList<>();
    
	public Map<Integer, String> getBindingStates() {
		return bingingStates;
	}    
	
    BatchUpdateBuilder(StaticDataProvider sdp, ApAccessPoint accessPoint, EntityRefHandler entityRefHandler,
			GroovyService groovyService, AccessPointDataService apDataService, ApScope scope,
			ApExternalSystemType extSystemType) {
		super(sdp, accessPoint, entityRefHandler, groovyService, apDataService, scope, extSystemType);
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
	
	/**
	 * Create entity reference
	 * @return Return reference for UpdateEntity
	 */
	abstract protected Object createBatchEntityRecordRef();
}
