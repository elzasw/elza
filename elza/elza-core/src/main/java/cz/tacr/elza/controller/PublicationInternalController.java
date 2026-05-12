package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.CopyPublication;
import cz.tacr.elza.controller.vo.CreatePublication;
import cz.tacr.elza.controller.vo.PublicationDetail;
import cz.tacr.elza.controller.vo.PublicationList;
import cz.tacr.elza.controller.vo.PublicationType;

/**
 * Internal REST API for the Elza UI.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag
 * {@code publication-internal}). All operations are currently stubs
 * returning {@link HttpStatus#NOT_IMPLEMENTED}; the actual logic will be
 * added once {@code ArrExportTypeService} and {@code ArrExportService} are
 * in place.
 *
 * Served under {@code /api/internal}, separate from the public publication
 * API at {@code /api/v1}. This separation is intentional — internal
 * endpoints are not an integration contract.
 */
@RestController
@RequestMapping("/api/internal")
public class PublicationInternalController implements PublicationInternalApi {

    // -----------------------------------------------------------------------
    // Publication type management (admin)
    // -----------------------------------------------------------------------

    @Override
    public ResponseEntity<List<PublicationType>> publicationTypeAdminListPublicationTypes() {
        // TODO: return all publication types from ARR_EXPORT_TYPE, including inactive.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<PublicationType> publicationTypeAdminCreatePublicationType(
            @RequestBody PublicationType publicationType) {
        // TODO: persist a new ARR_EXPORT_TYPE row; reject on duplicate code.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<PublicationType> publicationTypeAdminUpdatePublicationType(
            @PathVariable("id") Integer id,
            @RequestBody PublicationType publicationType) {
        // TODO: update ARR_EXPORT_TYPE; ignore id in body, use path id;
        //       reject on duplicate code conflict.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> publicationTypeAdminDeletePublicationType(
            @PathVariable("id") Integer id) {
        // TODO: hard-delete ARR_EXPORT_TYPE; reject 409 if any ARR_EXPORT
        //       still references it (deactivate via active=false is preferred).
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    // -----------------------------------------------------------------------
    // Fund-scoped publication operations
    // -----------------------------------------------------------------------

    @Override
    public ResponseEntity<PublicationList> fundPublicationListFundPublications(
            @PathVariable("fundId") Integer fundId,
            @RequestParam(value = "publicationTypeId", required = false) Integer publicationTypeId,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        // TODO: list ARR_EXPORT rows for the fund (newest first), with paging
        //       and optional filter by publication type.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<PublicationDetail> fundPublicationCreateFundPublication(
            @PathVariable("fundId") Integer fundId,
            @RequestBody CreatePublication createPublication) {
        // TODO: enqueue a new ARR_EXPORT against the current open version of
        //       the fund; reject 409 if an identical pending/prepared
        //       publication already exists.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Resource> fundPublicationDownloadFundPublication(
            @PathVariable("fundId") Integer fundId,
            @PathVariable("publicationId") Integer publicationId) {
        // TODO: stream the prepared XML for human inspection; do NOT advance
        //       state or touch last_fetched_at (that is the public API's job).
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> fundPublicationInvalidateFundPublication(
            @PathVariable("fundId") Integer fundId,
            @PathVariable("publicationId") Integer publicationId) {
        // TODO: delete the associated dms_file and transition the
        //       arr_export row to INVALIDATED; preserve the audit trail.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<PublicationDetail> fundPublicationCopyFundPublication(
            @PathVariable("fundId") Integer fundId,
            @PathVariable("publicationId") Integer publicationId,
            @RequestBody CopyPublication copyPublication) {
        // TODO: clone the existing export into the target type; reject 409
        //       on incompatible filter setting.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
