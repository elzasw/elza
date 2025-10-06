package cz.tacr.elza.cam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;

@Service
public class CamFactory {

    @Autowired
    private cz.tacr.elza.cam.v1.CamConnector camConnectorV1;

    @Autowired
    private cz.tacr.elza.cam.v2.CamConnector camConnectorV2;

	public ApiCamConnector getConnector(ApExternalSystem extSystem) {
		return extSystem.getType() == ApExternalSystemType.CAM_V2 ? camConnectorV2 : camConnectorV1;
	}
}
