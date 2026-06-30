package cz.tacr.elza.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.service.PublicationService;
import cz.tacr.elza.service.PublicationService.DownloadPayload;
import cz.tacr.elza.service.PublicationTypeService;
import jakarta.transaction.Transactional;

/**
 * Internal REST API for the Elza UI.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag
 * {@code publication-internal}). Per-endpoint permissions:
 *
 * <ul>
 *   <li>{@code /publication/types/*} — {@code ADMIN} (system-level
 *       configuration of target systems).</li>
 *   <li>{@code GET /fund/{fundId}/publication} — {@code FUND_RD} /
 *       {@code FUND_RD_ALL} / {@code ADMIN}.</li>
 *   <li>{@code GET /fund/{fundId}/publication/{id}/download} —
 *       {@code FUND_EXPORT} / {@code FUND_EXPORT_ALL} / {@code ADMIN}.</li>
 *   <li>Create / invalidate / copy — dynamic, see
 *       {@link cz.tacr.elza.service.PublicationService#authorizePublishToType}.
 *       {@code ADMIN} / {@code FUND_ADMIN} always pass; otherwise the type's
 *       {@code allowPerm*} flags decide which permission family is accepted.</li>
 * </ul>
 *
 * Internal endpoints share the {@code /api/v1} prefix with the rest of the
 * API; the separation from the public publication API is by tag, not URL
 * (so a future migration of an endpoint between the two doesn't require
 * a URL change).
 */
@RestController
@RequestMapping("/api/v1")
public class PublicationInternalController implements PublicationInternalApi {

	@Autowired
	private PublicationTypeService publicationTypeService;
	
	@Autowired
	private PublicationService publicationService;

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
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<List<PublicationType>> publicationTypeAdminListPublicationTypes() {
        List<PublicationType> result = publicationTypeService.listAll().stream().map(publicationTypeService::toVO).toList();

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
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<PublicationType> publicationTypeAdminCreatePublicationType(@RequestBody PublicationType publicationType) {
    	return ResponseEntity.ok(publicationTypeService.toVO(publicationTypeService.create(publicationType)));
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
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<PublicationType> publicationTypeAdminUpdatePublicationType(Integer id,
    		                                                                         @RequestBody PublicationType publicationType) {
    	return ResponseEntity.ok(publicationTypeService.toVO(publicationTypeService.update(id, publicationType)));
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
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<Void> publicationTypeAdminDeletePublicationType(Integer id) {
        publicationTypeService.delete(id);
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
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_RD_ALL, Permission.FUND_RD})
    public ResponseEntity<PublicationList> fundPublicationListFundPublications(@AuthParam(type = AuthParam.Type.FUND) Integer fundId,
            @RequestParam(value = "publicationTypeId", required = false) Integer publicationTypeId,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
    	return ResponseEntity.ok(publicationService.listByFund(fundId, publicationTypeId, offset, limit));
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
    	return ResponseEntity.ok(publicationService.create(fundId, createPublication.getPublicationTypeId()));
    }

    /**
     * GET /fund/{fundId}/publication/{publicationId}/download
     * Stream the prepared XML for human inspection.
     *
     * Internal endpoint — does NOT advance state or update last_fetched_at;
     * that is the responsibility of the public publication API.
     */
    @Override
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN, Permission.FUND_EXPORT_ALL, Permission.FUND_EXPORT})
    public ResponseEntity<Resource> fundPublicationDownloadFundPublication(@AuthParam(type = AuthParam.Type.FUND) Integer fundId, Integer publicationId) {
        DownloadPayload payload = publicationService.download(fundId, publicationId);
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
    	publicationService.invalidate(fundId, publicationId);
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
    	PublicationDetail publicationDetail = publicationService.copy(fundId, publicationId, copyPublication.getTargetPublicationTypeId());
        return ResponseEntity.ok(publicationDetail);
    }
}
