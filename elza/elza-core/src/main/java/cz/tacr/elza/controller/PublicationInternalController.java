package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import cz.tacr.elza.service.ExportTypeService;

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

	@Autowired
	private ExportTypeService exportTypeService;

    // -----------------------------------------------------------------------
    // Publication type management (admin)
    // -----------------------------------------------------------------------

    /**
     * GET /publication/types
     * Return all publication types, including inactive ones.
     *
     * @return The request has succeeded. (status code 200)
     */
    @Override
    public ResponseEntity<List<PublicationType>> publicationTypeAdminListPublicationTypes() {
        List<PublicationType> result = exportTypeService.listAll().stream().map(exportTypeService::toVO).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * POST /publication/types
     * Create a new publication type.
     *
     * @param publicationType  (required)
     * @return The request has succeeded. (status code 200)
     *         or The request conflicts with the current state of the server. (status code 409)
     */
    @Override
    public ResponseEntity<PublicationType> publicationTypeAdminCreatePublicationType(@RequestBody PublicationType publicationType) {
    	return ResponseEntity.ok(exportTypeService.toVO(exportTypeService.create(publicationType)));
    }

    /**
     * PUT /publication/types/{id}
     * Update an existing publication type.
     *
     * @param id  (required)
     * @param publicationType  (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     *         or The request conflicts with the current state of the server. (status code 409)
     */
    @Override
    public ResponseEntity<PublicationType> publicationTypeAdminUpdatePublicationType(Integer id, 
    		                                                                         @RequestBody PublicationType publicationType) {
    	return ResponseEntity.ok(exportTypeService.toVO(exportTypeService.update(id, publicationType)));
    }

    /**
     * DELETE /publication/types/{id}
     * Remove a publication type.  Deactivating via {
     *
     * @param id  (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     *         or The server cannot find the requested resource. (status code 404)
     *         or The request conflicts with the current state of the server. (status code 409)
     */
    @Override
    public ResponseEntity<Void> publicationTypeAdminDeletePublicationType(Integer id) {
        exportTypeService.delete(id);
        return ResponseEntity.ok().build();
    }

    // -----------------------------------------------------------------------
    // Fund-scoped publication operations
    // -----------------------------------------------------------------------

    /**
     * GET /fund/{fundId}/publication
     * Return the publications for a fund, newest first.  The UI must paginate — the list can grow large.
     *
     * @param fundId Fund ID (required)
     * @param publicationTypeId Optional filter on publication type. (optional)
     * @param offset Offset for pagination. (optional, default to 0)
     * @param limit Page size. (optional, default to 50)
     * @return The request has succeeded. (status code 200)
     */
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

    /**
     * POST /fund/{fundId}/publication
     * Enqueue a new publication for the current open version of the fund.  Rejected with 409 if the current fund version already has a publication of the same type in {
     *
     * @param fundId Fund ID (required)
     * @param createPublication  (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     *         or The request conflicts with the current state of the server. (status code 409)
     */
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
