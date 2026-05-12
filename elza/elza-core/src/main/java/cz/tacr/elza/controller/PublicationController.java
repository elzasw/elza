package cz.tacr.elza.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.AvailablePublications;
import cz.tacr.elza.controller.vo.PublicationStatusReport;

/**
 * Public REST API for the publication system.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag
 * {@code publication}). All operations are currently stubs returning
 * {@link HttpStatus#NOT_IMPLEMENTED}; the actual logic will be added as
 * the publication feature is implemented (see "Publikace archivního popisu"
 * spec).
 */
@RestController
@RequestMapping("/api/v1")
public class PublicationController implements PublicationApi {

    @Override
    public ResponseEntity<AvailablePublications> publicationGetAvailablePublications(
            @PathVariable("targetSystem") String targetSystem,
            @RequestParam(value = "lastTransaction", required = false) String lastTransaction) {
        // TODO: list prepared publications for the given target system,
        //       starting after `lastTransaction` (opaque export_seq cursor).
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Resource> publicationDownloadPublication(
            @PathVariable("id") Integer id) {
        // TODO: stream the prepared XML (application/xml). Bump last_fetched_at
        //       and transition PREPARED → FETCHED on first successful download.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<Void> publicationReportPublicationStatus(
            @PathVariable("id") Integer id,
            @RequestBody PublicationStatusReport publicationStatusReport) {
        // TODO: accept publication outcome (OK / ERROR) from the publication
        //       system. Idempotent on identical replays; allows recovery
        //       PUBLISH_ERROR → PUBLISHED.
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
