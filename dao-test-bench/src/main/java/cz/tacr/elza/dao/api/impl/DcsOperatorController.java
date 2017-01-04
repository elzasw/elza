package cz.tacr.elza.dao.api.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.dao.service.DcsRequestService;
import cz.tacr.elza.ws.core.v1.CoreServiceException;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.types.v1.RequestRevoked;

@RestController
@RequestMapping(value = "/op")
public class DcsOperatorController {

	@Autowired
	private DaoRequestsService daoRequestsService;

	@Autowired
	private DcsRequestService dcsRequestService;

	@RequestMapping(value = "/destr-requests/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public ResponseEntity<String> confirmDestrRequest(@PathVariable String requestIdentifier) {
		try {
			dcsRequestService.confirmRequest(requestIdentifier, true);
		} catch (IOException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/destr-requests/{requestIdentifier}/finished", method = RequestMethod.POST)
	public ResponseEntity<String> destrRequestFinished(@PathVariable String requestIdentifier) {
		try {
			daoRequestsService.destructionRequestFinished(requestIdentifier);
		} catch (CoreServiceException e) {
			return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/destr-requests/{requestIdentifier}/reject", method = RequestMethod.PUT)
	public ResponseEntity<String> rejectDestrRequest(@PathVariable String requestIdentifier) {
		try {
			dcsRequestService.deleteRequest(requestIdentifier, true);
		} catch (IOException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/destr-requests/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public ResponseEntity<String> destrRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) {
		try {
			RequestRevoked requestRevoked = new RequestRevoked();
			requestRevoked.setIdentifier(requestIdentifier);
			requestRevoked.setDescription(description);
			daoRequestsService.destructionRequestRevoked(requestRevoked);
		} catch (CoreServiceException e) {
			return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/trans-requests/{requestIdentifier}/confirm", method = RequestMethod.PUT)
	public ResponseEntity<String> confirmTransRequest(@PathVariable String requestIdentifier) {
		try {
			dcsRequestService.confirmRequest(requestIdentifier, false);
		} catch (IOException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/trans-requests/{requestIdentifier}/finished", method = RequestMethod.POST)
	public ResponseEntity<String> transRequestFinished(@PathVariable String requestIdentifier) {
		try {
			daoRequestsService.transferRequestFinished(requestIdentifier);
		} catch (CoreServiceException e) {
			return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/trans-requests/{requestIdentifier}/reject", method = RequestMethod.PUT)
	public ResponseEntity<String> rejectTransRequest(@PathVariable String requestIdentifier) {
		try {
			dcsRequestService.deleteRequest(requestIdentifier, false);
		} catch (IOException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}

	@RequestMapping(value = "/trans-requests/{requestIdentifier}/revoked", method = RequestMethod.POST)
	public ResponseEntity<String> transRequestRevoked(@PathVariable String requestIdentifier,
			@RequestParam(required = false) String description) {
		try {
			RequestRevoked requestRevoked = new RequestRevoked();
			requestRevoked.setIdentifier(requestIdentifier);
			requestRevoked.setDescription(description);
			daoRequestsService.transferRequestRevoked(requestRevoked);
		} catch (CoreServiceException e) {
			return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
		}
		return ResponseEntity.ok(requestIdentifier);
	}
}
