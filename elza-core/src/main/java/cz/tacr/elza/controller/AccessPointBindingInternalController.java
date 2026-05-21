package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.ExtHistory;
import cz.tacr.elza.controller.vo.ExtIssue;

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

    @Override
    public ResponseEntity<List<ExtIssue>> accessPointBindingGetBindingIssues(Integer bindingId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<ExtHistory> accessPointBindingGetBindingHistory(Integer bindingId, Integer offset, Integer limit) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
