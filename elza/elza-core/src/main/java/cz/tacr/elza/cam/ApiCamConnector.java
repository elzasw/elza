package cz.tacr.elza.cam;

import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.ApAdvanceSearchFilter;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;

public interface ApiCamConnector {

	ArchiveEntityResultListVO search(int from, int max, ApAdvanceSearchFilter filter, ApExternalSystem extlSystem);

	Integer takeArchiveEntity(String archiveEntityId, Integer scopeId, ApExternalSystem extlSystem);

	void takeRelArchiveEntities(ApState state, ApExternalSystem extSystem);

	void synchronizeEntity(ApState state, ApBindingState bindingState, ApExternalSystem extSystem);

	void connectArchiveEntity(String archiveEntityId, ApState state, ApExternalSystem extSystem, Boolean replace);
	
	String getDetailUrl(ApExternalSystem extSystem);

	void synchronizeAccessPointsForExternalSystem(String extSysCode);

	void exportApForce(ExtSyncsQueueItem queueItem);
}
