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
import cz.tacr.elza.dao.service.StorageDaoRequestService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

@RestController
@RequestMapping(value = "/request")
public class RequestController {

	@Autowired
	private StorageDaoRequestService storageDaoRequestService;

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmDestrRequest(@PathVariable String requestIdentifier) {
		storageDaoRequestService.confirmRequest(requestIdentifier, true);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/destr/{requestIdentifier}/finished", method = RequestMethod.POST)
	public void destrRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		String systemIdentifier = storageDaoRequestService.getSystemIdentifier(requestIdentifier, true);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.destructionRequestFinished(requestIdentifier);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr/{requestIdentifier}/reject", method = RequestMethod.DELETE)
	public void rejectDestrRequest(@PathVariable String requestIdentifier) {
		storageDaoRequestService.deleteRequest(requestIdentifier, true);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/destr/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void destrRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestIdentifier);
		requestRevoked.setDescription(description);
		String systemIdentifier = storageDaoRequestService.getSystemIdentifier(requestIdentifier, true);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.destructionRequestRevoked(requestRevoked);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmTransRequest(@PathVariable String requestIdentifier) {
		storageDaoRequestService.confirmRequest(requestIdentifier, false);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/trans/{requestIdentifier}/finished", method = RequestMethod.POST)
	public void transRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		String systemIdentifier = storageDaoRequestService.getSystemIdentifier(requestIdentifier, false);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.transferRequestFinished(requestIdentifier);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans/{requestIdentifier}/reject", method = RequestMethod.DELETE)
	public void rejectTransRequest(@PathVariable String requestIdentifier) {
		storageDaoRequestService.deleteRequest(requestIdentifier, false);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/trans/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void transRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestIdentifier);
		requestRevoked.setDescription(description);
		String systemIdentifier = storageDaoRequestService.getSystemIdentifier(requestIdentifier, false);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.transferRequestRevoked(requestRevoked);
	}
}