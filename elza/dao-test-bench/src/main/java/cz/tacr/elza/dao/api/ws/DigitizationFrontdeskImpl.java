package cz.tacr.elza.dao.api.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.dao.service.StorageDigitizationRequestService;
import cz.tacr.elza.ws.digitization.v1.DigitizationFrontdesk;
import cz.tacr.elza.ws.digitization.v1.DigitizationServiceException;
import cz.tacr.elza.ws.types.v1.DigitizationRequest;
import jakarta.jws.WebService;

@Service
@WebService(name = DigitizationFrontdeskImpl.NAME,
		portName = DigitizationFrontdeskImpl.NAME,
		serviceName = DigitizationFrontdeskImpl.NAME,
		targetNamespace = "http://elza.tacr.cz/ws/digitization/v1",
		endpointInterface = "cz.tacr.elza.ws.digitization.v1.DigitizationFrontdesk")
public class DigitizationFrontdeskImpl implements DigitizationFrontdesk {

	public static final String NAME = "DigitizationFrontdesk";

	@Autowired
	private StorageDigitizationRequestService storageDigitizationRequestService;

	@Override
	public String postRequest(DigitizationRequest digitizationRequest) throws DigitizationServiceException {
		storageDigitizationRequestService.checkRejectMode();
		try {
			return storageDigitizationRequestService.createRequest(digitizationRequest);
		} catch (DaoComponentException e) {
			throw new DigitizationServiceException(e.getMessage(), e.getCause());
		}
	}
}