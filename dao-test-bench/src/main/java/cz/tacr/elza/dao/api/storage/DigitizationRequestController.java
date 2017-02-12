package cz.tacr.elza.dao.api.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.bo.resource.DigitizationRequestInfo;
import cz.tacr.elza.dao.common.CoreServiceProvider;
import cz.tacr.elza.dao.service.ResourceService;
import cz.tacr.elza.dao.service.StorageDigitizationRequestService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoDigitizationService;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DigitizationRequestResult;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

@RestController
@RequestMapping(value = "/request/digi")
public class DigitizationRequestController {

	@Autowired
	private StorageDigitizationRequestService storageDigitizationRequestService;

	@Autowired
	private ResourceService resourceService;

	/**
	 * Confirms digitization request and sends notification to external system. Digitalized packages must be specified.
	 * Connection to external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{requestIdentifier}/confirm/{packageIdentifiers}", method = RequestMethod.POST)
	public void digiRequestFinished(@PathVariable String requestIdentifier, @PathVariable String[] packageIdentifiers)
			throws CoreServiceException {
		DigitizationRequestInfo requestInfo = storageDigitizationRequestService.confirmRequest(requestIdentifier);
		DaoImport daoImport = resourceService.getDaoImport(packageIdentifiers);
		DigitizationRequestResult result = new DigitizationRequestResult();
		result.setIdentifier(requestInfo.getIdentifier());
		result.setDaoImport(daoImport);
		DaoDigitizationService service = CoreServiceProvider.getDaoDigitizationService(requestInfo.getSystemIdentifier());
		service.digitizationRequestFinished(result);
	}

	/**
	 * Rejects digitization request and sends notification to external system.
	 * Connection to external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{requestIdentifier}/reject", method = RequestMethod.POST)
	public void digiRequestReject(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description)
			throws CoreServiceException {
		DigitizationRequestInfo requestInfo = storageDigitizationRequestService.rejectRequest(requestIdentifier);
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestInfo.getIdentifier());
		requestRevoked.setDescription(description);
		DaoDigitizationService service = CoreServiceProvider.getDaoDigitizationService(requestInfo.getSystemIdentifier());
		service.digitizationRequestRevoked(requestRevoked);
	}
}
