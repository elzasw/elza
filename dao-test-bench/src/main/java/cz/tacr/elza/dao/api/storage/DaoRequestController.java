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

import cz.tacr.elza.dao.bo.resource.DaoRequestInfo;
import cz.tacr.elza.dao.common.CoreServiceProvider;
import cz.tacr.elza.dao.service.StorageDaoRequestService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

@RestController
@RequestMapping(value = "/request/dao")
public class DaoRequestController {

	@Autowired
	private StorageDaoRequestService storageDaoRequestService;

	/**
	 * Confirms destruction request and sends notification to external system. Connection to
	 * external system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/destr/{requestIdentifier}/confirm", method = RequestMethod.POST)
	public void destrRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		DaoRequestInfo requestInfo = storageDaoRequestService.confirmRequest(requestIdentifier, true);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(requestInfo.getSystemIdentifier());
		service.destructionRequestFinished(requestInfo.getRequestIdentifier());
	}

	/**
	 * Rejects destruction request and sends notification to external system. Connection to external
	 * system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/destr/{requestIdentifier}/reject", method = RequestMethod.POST)
	public void rejectDestrRequest(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		DaoRequestInfo requestInfo = storageDaoRequestService.rejectRequest(requestIdentifier, true);
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestInfo.getRequestIdentifier());
		requestRevoked.setDescription(description);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(requestInfo.getSystemIdentifier());
		service.destructionRequestRevoked(requestRevoked);
	}

	/**
	 * Confirms transfer request and sends notification to external system. Connection to external
	 * system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/trans/{requestIdentifier}/confirm", method = RequestMethod.POST)
	public void confirmTransRequest(@PathVariable String requestIdentifier) throws CoreServiceException {
		DaoRequestInfo requestInfo = storageDaoRequestService.confirmRequest(requestIdentifier, false);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(requestInfo.getSystemIdentifier());
		service.transferRequestFinished(requestInfo.getRequestIdentifier());
	}

	/**
	 * Rejects transfer request and sends notification to external system. Connection to external
	 * system must be defined in /{repositoryIdentifier}/external-systems-config.yaml.
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/trans/{requestIdentifier}/reject", method = RequestMethod.POST)
	public void rejectTransRequest(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		DaoRequestInfo requestInfo = storageDaoRequestService.rejectRequest(requestIdentifier, false);
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestInfo.getRequestIdentifier());
		requestRevoked.setDescription(description);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(requestInfo.getSystemIdentifier());
		service.transferRequestRevoked(requestRevoked);
	}
}