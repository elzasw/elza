package cz.tacr.elza.dao.api.storage;

import java.util.Arrays;

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
import cz.tacr.elza.dao.service.DcsDaoRequestService;
import cz.tacr.elza.dao.service.DcsResourceService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

@RestController
@RequestMapping(value = "/cmd")
public class DcsCommandController {

	@Autowired
	private DcsDaoRequestService dcsDaoRequestService;

	@Autowired
	private DcsResourceService dcsDaoResourceService;

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmDestrRequest(@PathVariable String requestIdentifier) {
		dcsDaoRequestService.confirmRequest(requestIdentifier, true);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/finished", method = RequestMethod.POST)
	public void destrRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		String systemIdentifier = dcsDaoRequestService.getSystemIdentifier(requestIdentifier, true);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.destructionRequestFinished(requestIdentifier);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/reject", method = RequestMethod.DELETE)
	public void rejectDestrRequest(@PathVariable String requestIdentifier) {
		dcsDaoRequestService.deleteRequest(requestIdentifier, true);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void destrRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestIdentifier);
		requestRevoked.setDescription(description);
		String systemIdentifier = dcsDaoRequestService.getSystemIdentifier(requestIdentifier, true);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.destructionRequestRevoked(requestRevoked);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmTransRequest(@PathVariable String requestIdentifier) {
		dcsDaoRequestService.confirmRequest(requestIdentifier, false);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/finished", method = RequestMethod.POST)
	public void transRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		String systemIdentifier = dcsDaoRequestService.getSystemIdentifier(requestIdentifier, false);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.transferRequestFinished(requestIdentifier);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/reject", method = RequestMethod.DELETE)
	public void rejectTransRequest(@PathVariable String requestIdentifier) {
		dcsDaoRequestService.deleteRequest(requestIdentifier, false);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void transRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestIdentifier);
		requestRevoked.setDescription(description);
		String systemIdentifier = dcsDaoRequestService.getSystemIdentifier(requestIdentifier, false);
		DaoRequestsService service = CoreServiceProvider.getDaoRequestsService(systemIdentifier);
		service.transferRequestRevoked(requestRevoked);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/import/{systemIdentifier}/{packageIdentifiers}", method = RequestMethod.POST)
	public void transRequestRevoked(@PathVariable String systemIdentifier,
			@PathVariable String[] packageIdentifiers) throws CoreServiceException {
		DaoImport daoImport = dcsDaoResourceService.getDaoImport(Arrays.asList(packageIdentifiers));
		DaoService service = CoreServiceProvider.getDaoService(systemIdentifier);
		service._import(daoImport);
	}
}