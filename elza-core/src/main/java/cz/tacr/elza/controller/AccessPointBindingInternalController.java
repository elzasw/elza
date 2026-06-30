package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.ExtHistory;
import cz.tacr.elza.controller.vo.ExtIssue;
import cz.tacr.elza.service.ExternalSystemService;
import jakarta.transaction.Transactional;

/**
 * Internal REST API for the Elza UI — CAMv2 binding issues and history.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag
 * {@code accesspoint-internal}). All operations are currently stubs returning
 * {@link HttpStatus#NOT_IMPLEMENTED}; real logic will land once the
 * {@code ap_binding_issue} / {@code ap_binding_participant} tables and the
 * corresponding services are in place.
 *
 * Served under {@code /api/v1} alongside the public access-point API. Not
 * intended for external integration; backwards compatibility is not
 * guaranteed.
 */
@RestController
@RequestMapping("/api/v1")
public class AccessPointBindingInternalController implements AccesspointInternalApi {

    @Autowired
    private ExternalSystemService externalSystemService;

    /**
     * GET /binding/{bindingId}/issues
     * List CAM-side issues attached to a binding.
     *
     * @param bindingId id of binding (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
    public ResponseEntity<List<ExtIssue>> accessPointBindingGetBindingIssues(Integer bindingId) {
		return ResponseEntity.ok(externalSystemService.findBindingIssues(bindingId));
    }

    /**
     * GET /binding/{bindingId}/history
     * Paginated history of binding revisions, newest first, built from ap_binding_state and ap_binding_participant. Participants within each revision are ordered by lastChange (ascending).
     *
     * @param bindingId id of binding (required)
     * @param offset  (optional, default to 0)
     * @param limit server caps at 100 (optional, default to 100)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
    public ResponseEntity<ExtHistory> accessPointBindingGetBindingHistory(Integer bindingId, Integer offset, Integer limit) {
		return ResponseEntity.ok(externalSystemService.findBindingHistory(bindingId, offset, limit));
    }
}
