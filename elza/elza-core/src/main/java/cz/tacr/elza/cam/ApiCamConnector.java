package cz.tacr.elza.cam;

import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;

public interface ApiCamConnector {

	ArchiveEntityResultListVO search(int from, int max, SearchFilterVO filter, ApExternalSystem extlSystem);

	Integer takeArchiveEntity(String archiveEntityId, Integer scopeId, ApExternalSystem extlSystem);

	void takeRelArchiveEntities(ApState state, ApExternalSystem extSystem);

	void synchronizeEntity(ApState state, ApBindingState bindingState, ApExternalSystem extSystem);

	void connectArchiveEntity(String archiveEntityId, ApState state, ApExternalSystem extSystem, Boolean replace);
	
	String getDetailUrl(ApExternalSystem extSystem);

}
