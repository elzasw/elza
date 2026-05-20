package cz.tacr.elza.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.CopyPublication;
import cz.tacr.elza.controller.vo.CreatePublication;
import cz.tacr.elza.controller.vo.PublicationDetail;
import cz.tacr.elza.controller.vo.PublicationList;
import cz.tacr.elza.controller.vo.PublicationType;
import cz.tacr.elza.service.PublicationService;
import cz.tacr.elza.service.PublicationTypeService;

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
@RequestMapping("/api/v1")
public class PublicationInternalController implements PublicationInternalApi {

	@Autowired
	private PublicationTypeService exportTypeService;
	
	@Autowired
	private PublicationService exportService;

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
    public ResponseEntity<PublicationList> fundPublicationListFundPublications(Integer fundId,
            @RequestParam(value = "publicationTypeId", required = false) Integer publicationTypeId,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
    	return ResponseEntity.ok(exportService.listByFund(fundId, publicationTypeId, offset, limit));
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
    public ResponseEntity<PublicationDetail> fundPublicationCreateFundPublication(Integer fundId, @RequestBody CreatePublication createPublication) {
    	return ResponseEntity.ok(exportService.create(fundId, createPublication.getPublicationTypeId()));
    }

    /**
     * GET /fund/{fundId}/publication/{publicationId}/download
     * Stream the prepared XML for human inspection.
     *
     * Internal endpoint — does NOT advance state or update last_fetched_at;
     * that is the responsibility of the public publication API.
     */
    @Override
    public ResponseEntity<Resource> fundPublicationDownloadFundPublication(Integer fundId, Integer publicationId) {
        PublicationService.DownloadPayload payload = exportService.download(fundId, publicationId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(payload.getFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(payload.getResource());
    }

    /**
     * DELETE /fund/{fundId}/publication/{publicationId}
     * Invalidate a publication.  The associated DMS file is deleted from disk; the {
     *
     * @param fundId Fund ID (required)
     * @param publicationId Publication ID (ARR_EXPORT.export_id). (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     *         or Access is forbidden. (status code 403)
     *         or The server cannot find the requested resource. (status code 404)
     */
    @Override
    public ResponseEntity<Void> fundPublicationInvalidateFundPublication(Integer fundId,Integer publicationId) {
    	exportService.invalidate(fundId, publicationId);
    	return ResponseEntity.ok().build();
    }

    /**
     * POST /fund/{fundId}/publication/{publicationId}/copy
     * Copy an existing publication into another target system.  The target publication type must have a compatible filter setting (same {
     *
     * @param fundId Fund ID (required)
     * @param publicationId Publication ID (ARR_EXPORT.export_id). (required)
     * @param copyPublication  (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     *         or The request conflicts with the current state of the server. (status code 409)
     */
    @Override
    public ResponseEntity<PublicationDetail> fundPublicationCopyFundPublication(Integer fundId, Integer publicationId, @RequestBody CopyPublication copyPublication) {
    	PublicationDetail publicationDetail = exportService.copy(fundId, publicationId, copyPublication.getTargetPublicationTypeId());
        return ResponseEntity.ok(publicationDetail);
    }
}
