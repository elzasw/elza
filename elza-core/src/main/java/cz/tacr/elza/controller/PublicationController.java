package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.vo.AvailablePublications;
import cz.tacr.elza.controller.vo.PublicationStatusReport;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.service.PublicationService;
import cz.tacr.elza.service.PublicationService.DownloadPayload;
import jakarta.transaction.Transactional;

/**
 * Public REST API for the publication system.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag
 * {@code publication}). All endpoints require {@code FUND_PUBLISH_ALL}
 * (or {@code ADMIN}); see the {@link AuthMethod @AuthMethod} annotations
 * on the methods. Behaviour follows the "Publikace archivního popisu"
 * specification.
 */
@RestController
@RequestMapping("/api/v1")
public class PublicationController implements PublicationApi {

	@Autowired
	private PublicationService publicationService;

    /**
     * GET /publications/available/{targetSystem}
     * Get the list of prepared XML exports for a given target system.  On the first call `lastTransaction` is omitted. 
     * The response carries `nextTransaction`, which the caller stores and sends in the next request as `lastTransaction`.
     * This way only new exports since the previous call are returned.  At most 100 records are returned. 
     * If multiple prepared exports exist for the same fund, only the most recent one is included.  
     * If the publication type (target system) is inactive, the call returns 403 — 
     * information about prepared exports is not exposed in that case.
     *
     * @param targetSystem Publication type code (ARR_EXPORT_TYPE.code), which also identifies the target system. (required)
     * @param lastTransaction Opaque key of the last already-processed transaction. Omitted on the first call. The caller must not parse the key — Elza reserves the right to change its format in the future. (optional)
     * @return The request has succeeded. (status code 200)
     *         or Access is forbidden. (status code 403)
     *         or The server cannot find the requested resource. (status code 404)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN, UsrPermission.Permission.FUND_PUBLISH_ALL})
    public ResponseEntity<AvailablePublications> publicationGetAvailablePublications(String targetSystem,
            @RequestParam(value = "lastTransaction", required = false) String lastTransaction) {
        return ResponseEntity.ok(publicationService.listAvailable(targetSystem, lastTransaction));
    }

    /**
     * GET /publications/download/{id}
     * Download the prepared XML.  The client should only call this with an ID it received from `/available`.
     * The download succeeds as long as the XML file is physically present on the server; each successful download updates 
     * the internal audit trail of the last fetch.  The file is returned as `application/xml` with header 
     * `Content-Disposition: attachment; filename`\"{mark}-{fundNumber}-{publicationId}.xml\"`. 
     * If the fund has neither `mark` nor `fundNumber` set, the fallback `fundId-{fundId}-{publicationId}.xml` is used.
     * HTTP responses: 
     * - 200 + XML — the file is available 
     * - 403       — the publication type has been deactivated 
     * - 404       — unknown ID, or an ID in an internal state that is not exposed to the client (e.g. preparation in progress — such IDs are never returned by `/available` and the client should not encounter them) 
     * - 410       — the file is permanently unavailable to the client (publication invalidated by the user, or retention expired)
     *
     * @param id Publication identifier (ARR_EXPORT.export_id). (required)
     * @return The request has succeeded. (status code 200)
     *         or Access is forbidden. (status code 403)
     *         or The server cannot find the requested resource. (status code 404)
     *         or Client error (status code 410)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN, UsrPermission.Permission.FUND_PUBLISH_ALL})
    public ResponseEntity<Resource> publicationDownloadPublication(Integer id) {
        DownloadPayload payload = publicationService.downloadAvailable(id);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        FileDownload.addContentDispositionAsAttachment(headers, payload.getFileName());
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        return new ResponseEntity<>(payload.getResource(), headers, HttpStatus.OK);
    }

    /**
     * POST /publications/status/{id}
     * Report the publication outcome from the publication system.  The client reports the outcome of a publication it has been working with — typically after a successful &#x60;/download/{id}&#x60;, but a report from a publication that was prepared and not yet marked as fetched is also accepted (e.g. when the download response was lost in transit but the publication system still processed the file).  - status OK    — publication succeeded - status ERROR — publication failed; the outcome may later be reported again with status OK (recovery) or ERROR (updated error message)  The call is idempotent: a repeated call with an identical body returns 200 OK without changing internal state.
     *
     * @param id Publication identifier (ARR_EXPORT.export_id). (required)
     * @param publicationStatusReport  (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     *         or The request conflicts with the current state of the server. (status code 409)
     */
    @Override
    @Transactional
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN, UsrPermission.Permission.FUND_PUBLISH_ALL})
    public ResponseEntity<Void> publicationReportPublicationStatus(Integer id, @RequestBody PublicationStatusReport publicationStatusReport) {
        publicationService.reportStatus(id, publicationStatusReport);
        return ResponseEntity.ok().build();
    }
}
