package cz.tacr.elza.dao.api.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.service.DcsDaoRequestService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

@RestController
@RequestMapping(value = "/op")
public class DcsOperatorController {

	@Autowired
	private DaoRequestsService daoRequestsService;

	@Autowired
	private DcsDaoRequestService dcsRequestService;

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmDestrRequest(@PathVariable String requestIdentifier) {
		dcsRequestService.confirmRequest(requestIdentifier, true);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/finished", method = RequestMethod.POST)
	public void destrRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		daoRequestsService.destructionRequestFinished(requestIdentifier);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/reject", method = RequestMethod.DELETE)
	public void rejectDestrRequest(@PathVariable String requestIdentifier) {
		dcsRequestService.deleteRequest(requestIdentifier, true);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/destr-requests/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void destrRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestIdentifier);
		requestRevoked.setDescription(description);
		daoRequestsService.destructionRequestRevoked(requestRevoked);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public void confirmTransRequest(@PathVariable String requestIdentifier) {
		dcsRequestService.confirmRequest(requestIdentifier, false);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/finished", method = RequestMethod.POST)
	public void transRequestFinished(@PathVariable String requestIdentifier) throws CoreServiceException {
		daoRequestsService.transferRequestFinished(requestIdentifier);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/reject", method = RequestMethod.DELETE)
	public void rejectTransRequest(@PathVariable String requestIdentifier) {
		dcsRequestService.deleteRequest(requestIdentifier, false);
	}

	@ResponseBody
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/trans-requests/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public void transRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) throws CoreServiceException {
		RequestRevoked requestRevoked = new RequestRevoked();
		requestRevoked.setIdentifier(requestIdentifier);
		requestRevoked.setDescription(description);
		daoRequestsService.transferRequestRevoked(requestRevoked);
	}
}