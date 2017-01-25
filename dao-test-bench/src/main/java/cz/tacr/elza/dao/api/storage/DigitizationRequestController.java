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
	 * Confirms digitization request only for storage (no notification send to external system).
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmDigiRequest(@PathVariable String requestIdentifier) {
		storageDigitizationRequestService.deleteRequest(requestIdentifier);
	}

	/**
	 * Sends notification about confirmed digitization to external system. Resulting packages must be specified.
	 * This action should be called afters success of {@link #confirmDigiRequest(String)}.
	 * Connection to external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{requestIdentifier}/finished/{packageIdentifiers}", method = RequestMethod.POST)
	public void digiRequestFinished(@PathVariable String requestIdentifier, @PathVariable String[] packageIdentifiers)
			throws CoreServiceException {
		DaoImport daoImport = resourceService.getDaoImport(packageIdentifiers);
		DigitizationRequestResult result = new DigitizationRequestResult();
		result.setIdentifier(storageDigitizationRequestService.getExtIdentifier(requestIdentifier));
		result.setDaoImport(daoImport);
		String systemIdentifier = storageDigitizationRequestService.getSystemIdentifier(requestIdentifier);
		DaoDigitizationService service = CoreServiceProvider.getDaoDigitizationService(systemIdentifier);
		service.digitizationRequestFinished(result);
	}

	/**
	 * Rejects digitization request only for storage (no notification send to external system).
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/{requestIdentifier}/reject", method = RequestMethod.PUT)
	public void rejectDigiRequest(@PathVariable String requestIdentifier) {
		storageDigitizationRequestService.deleteRequest(requestIdentifier);
	}

	/**
	 * Sends notification about rejected digitization to external system.
	 * This action should be called afters success of {@link #rejectDigiRequest(String)}.
	 * Connection to external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void digiRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(storageDigitizationRequestService.getExtIdentifier(requestIdentifier));
		requestRevoked.setDescription(description);
		String systemIdentifier = storageDigitizationRequestService.getSystemIdentifier(requestIdentifier);
		DaoDigitizationService service = CoreServiceProvider.getDaoDigitizationService(systemIdentifier);
		service.digitizationRequestRevoked(requestRevoked);
	}
}
