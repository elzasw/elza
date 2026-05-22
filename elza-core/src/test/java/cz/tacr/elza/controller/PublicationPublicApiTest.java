package cz.tacr.elza.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.ArrExport;
import cz.tacr.elza.domain.ArrExportType;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.test.controller.vo.AvailablePublication;
import cz.tacr.elza.test.controller.vo.AvailablePublications;
import cz.tacr.elza.test.controller.vo.ConnectionType;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.PublicationReportStatus;
import cz.tacr.elza.test.controller.vo.PublicationStatusReport;
import cz.tacr.elza.test.controller.vo.PublicationType;

/**
 * Tests for the public publication API ({@code /api/v1/publications/*}).
 *
 * Each test exercises one endpoint of {@link PublicationController} from end
 * to end through the generated REST client {@code publicationApi}. The async
 * export generator is stopped per test; the {@code arr_export} states needed
 * for each scenario are seeded directly through repositories — the public
 * "create" path only yields NEW exports, so it cannot drive coverage on its
 * own.
 */
public class PublicationPublicApiTest extends AbstractControllerTest {
    /**
     * Covers {@code GET /publications/available/{targetSystem}}:
     *
     *   • happy path with multiple funds in different observable states,
     *   • cursor advances past skipped duplicates (dedupe per fund),
     *   • exports without a downloadable file are excluded,
     *   • internal-only states (NEW / PREPARE_ERROR / INVALIDATED) are hidden,
     *   • cross-target-system isolation,
     *   • empty result echoes the input cursor,
     *   • invalid cursor → fresh start (no 400),
     *   • inactive type → 403, unknown type → 404.
     */
    @Test
    public void publicationGetAvailablePublicationsTest() {
        asyncRequestService.stop();
        TestContext ctx = setupUserAndFund();
        Integer fundA = ctx.fundId;
        Integer fundB = createFund("pub-fund-B", "pub-fund-B-code").getId();
        Integer fundC = createFund("pub-fund-C", "pub-fund-C-code").getId();
        Integer fundD = createFund("pub-fund-D", "pub-fund-D-code").getId();

        ArrExportType active = createTypeAndFetch(ctx, "TST_AVAIL_ACTIVE");
        ArrExportType other  = createTypeAndFetch(ctx, "TST_AVAIL_OTHER");

        // ------------------------------------------------------------------
        // 1. Unknown target system → 404.
        // ------------------------------------------------------------------
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationGetAvailablePublications("DOES_NOT_EXIST", null));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        // ------------------------------------------------------------------
        // 2. Empty listing on a fresh active type → default "0" cursor.
        // ------------------------------------------------------------------
        AvailablePublications empty = publicationApi.publicationGetAvailablePublications("TST_AVAIL_ACTIVE", null);
        assertTrue(empty.getItems().isEmpty());
        assertEquals("0", empty.getNextTransaction());

        // ------------------------------------------------------------------
        // 3. Seed exports covering every relevant axis.
        //    Visible to the API (state in PREPARED/FETCHED/PUBLISHED/PUBLISH_ERROR,
        //    file present, target system matches):
        // ------------------------------------------------------------------
        prepareExport(fundA, active, ArrExport.State.PREPARED, 10L, true); // older duplicate for fund A
        ArrExport latestA  = prepareExport(fundA, active, ArrExport.State.PREPARED,      20L, true);
        ArrExport fetchedB = prepareExport(fundB, active, ArrExport.State.FETCHED,       30L, true);
        ArrExport pubC     = prepareExport(fundC, active, ArrExport.State.PUBLISHED,     40L, true);
        ArrExport errD     = prepareExport(fundD, active, ArrExport.State.PUBLISH_ERROR, 45L, true);

        // File missing — observable state but retention has purged the XML.
        prepareExport(fundA, active, ArrExport.State.PUBLISHED, 50L, false);

        // Internal-only states — must never appear in the public listing.
        prepareExport(fundB, active, ArrExport.State.NEW,           60L, true);
        prepareExport(fundB, active, ArrExport.State.PREPARE_ERROR, 70L, true);
        prepareExport(fundB, active, ArrExport.State.INVALIDATED,   80L, false);

        // Different target system — must not leak across systems.
        prepareExport(fundC, other, ArrExport.State.PREPARED, 90L, true);

        // ------------------------------------------------------------------
        // 4. Full listing: 4 items (deduped per fund, ASC by exportSeq).
        //    fund A keeps the latest (seq=20), not seq=10.
        //    Cursor advances to the last RAW row (seq=45), not the last
        //    deduped one — replays must not return skipped duplicates.
        // ------------------------------------------------------------------
        AvailablePublications all = publicationApi
                .publicationGetAvailablePublications("TST_AVAIL_ACTIVE", null);
        assertEquals(4, all.getItems().size());
        List<Integer> ids = all.getItems().stream()
                .map(AvailablePublication::getPublicationId)
                .toList();
        assertTrue(ids.contains(latestA.getExportId()));
        assertTrue(ids.contains(fetchedB.getExportId()));
        assertTrue(ids.contains(pubC.getExportId()));
        assertTrue(ids.contains(errD.getExportId()));
        assertEquals("45", all.getNextTransaction());

        // ------------------------------------------------------------------
        // 5. Cursor filter: lastTransaction=30 → only seq>30 returned.
        // ------------------------------------------------------------------
        AvailablePublications afterB = publicationApi.publicationGetAvailablePublications("TST_AVAIL_ACTIVE", "30");
        assertEquals(2, afterB.getItems().size());
        List<Integer> afterIds = afterB.getItems().stream()
                .map(AvailablePublication::getPublicationId)
                .toList();
        assertTrue(afterIds.contains(pubC.getExportId()));
        assertTrue(afterIds.contains(errD.getExportId()));
        assertEquals("45", afterB.getNextTransaction());

        // ------------------------------------------------------------------
        // 6. Cursor past the end → empty result, input cursor echoed back.
        // ------------------------------------------------------------------
        AvailablePublications echo = publicationApi.publicationGetAvailablePublications("TST_AVAIL_ACTIVE", "999");
        assertTrue(echo.getItems().isEmpty());
        assertEquals("999", echo.getNextTransaction());

        // ------------------------------------------------------------------
        // 7. Malformed cursor → treated as a fresh start (no 400).
        //    A publication system that lost its state can resume cleanly.
        // ------------------------------------------------------------------
        AvailablePublications garbage = publicationApi.publicationGetAvailablePublications("TST_AVAIL_ACTIVE", "garbage");
        assertEquals(4, garbage.getItems().size());

        // ------------------------------------------------------------------
        // 8. Other target system listing must not include the active-type rows.
        // ------------------------------------------------------------------
        AvailablePublications onlyOther = publicationApi.publicationGetAvailablePublications("TST_AVAIL_OTHER", null);
        assertEquals(1, onlyOther.getItems().size());
        assertEquals(fundC, onlyOther.getItems().get(0).getFundId());

        // ------------------------------------------------------------------
        // 9. Deactivating the type hides everything behind 403, even though
        //    the exports themselves still exist.
        // ------------------------------------------------------------------
        deactivateType(active);
        HttpClientErrorException forbidden = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationGetAvailablePublications("TST_AVAIL_ACTIVE", null));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    /**
     * Covers {@code GET /publications/download/{id}}:
     *
     *   • happy path PREPARED → FETCHED, file streamed back,
     *   • every download refreshes {@code lastFetchedAt},
     *   • repeated download on FETCHED / PUBLISHED / PUBLISH_ERROR keeps the state,
     *   • internal-only states (NEW / PREPARE_ERROR / INVALIDATED) → 404,
     *   • observable state but file purged by retention → 410,
     *   • inactive type → 403, unknown id → 404.
     */
    @Test
    public void publicationDownloadPublicationTest() throws IOException {
        TestContext ctx = setupUserAndFund();
        ArrExportType active = createTypeAndFetch(ctx, "TST_DL_ACTIVE");
        Integer fund = ctx.fundId;

        // ------------------------------------------------------------------
        // 1. Unknown id → 404.
        // ------------------------------------------------------------------
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationDownloadPublication(999_999));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        // ------------------------------------------------------------------
        // 2. Internal-only states must not be observable from the public API.
        //    countOutstanding does not block these since we seed through the
        //    repository, so they can share a fund.
        // ------------------------------------------------------------------
        ArrExport newE = prepareExport(fund, active, ArrExport.State.NEW,           null, false);
        ArrExport peE  = prepareExport(fund, active, ArrExport.State.PREPARE_ERROR, null, false);
        ArrExport invE = prepareExport(fund, active, ArrExport.State.INVALIDATED,   1L,   false);
        for (Integer hidden : List.of(newE.getExportId(), peE.getExportId(), invE.getExportId())) {
            HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                    () -> publicationApi.publicationDownloadPublication(hidden));
            assertEquals("publication id " + hidden + " must be hidden from the public API", HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        // ------------------------------------------------------------------
        // 3. File purged by retention (observable state, no DmsFile) → 410.
        // ------------------------------------------------------------------
        ArrExport goneE = prepareExport(fund, active, ArrExport.State.PUBLISHED, 10L, false);
        HttpClientErrorException gone = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationDownloadPublication(goneE.getExportId()));
        assertEquals(HttpStatus.GONE, gone.getStatusCode());

        // ------------------------------------------------------------------
        // 4. Happy path: PREPARED + file → 200, body returned,
        //    state transitions to FETCHED, lastFetchedAt populated.
        // ------------------------------------------------------------------
        ArrExport prepE = prepareExport(fund, active, ArrExport.State.PREPARED, 20L, true);
        OffsetDateTime before = OffsetDateTime.now();

        Resource body = publicationApi.publicationDownloadPublication(prepE.getExportId());
        assertNotNull(body);
        String xml = new String(body.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue("body should be the XML written by prepareExport", xml.contains("state=\"PREPARED\""));

        ArrExport afterFirst = exportRepository.findById(prepE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.FETCHED, afterFirst.getState());
        assertNotNull(afterFirst.getLastFetchedAt());
        assertTrue("lastFetchedAt must be stamped on the first download", !afterFirst.getLastFetchedAt().isBefore(before));

        // ------------------------------------------------------------------
        // 5. Every subsequent download refreshes lastFetchedAt — we prove it
        //    by forcing the timestamp into the past and checking it moves
        //    forward (a same-millisecond replay would be a flaky check).
        // ------------------------------------------------------------------
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        afterFirst.setLastFetchedAt(oneHourAgo);
        exportRepository.save(afterFirst);

        publicationApi.publicationDownloadPublication(prepE.getExportId());

        ArrExport afterSecond = exportRepository.findById(prepE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.FETCHED, afterSecond.getState());
        assertTrue("lastFetchedAt must be refreshed on every download, not only on the PREPARED→FETCHED transition",
        		afterSecond.getLastFetchedAt().isAfter(oneHourAgo.plusMinutes(30)));

        // ------------------------------------------------------------------
        // 6. PUBLISHED + file → still downloadable; state remains PUBLISHED.
        // ------------------------------------------------------------------
        ArrExport pubE = prepareExport(fund, active, ArrExport.State.PUBLISHED, 30L, true);
        publicationApi.publicationDownloadPublication(pubE.getExportId());
        assertEquals(ArrExport.State.PUBLISHED, exportRepository.findById(pubE.getExportId()).orElseThrow().getState());

        // ------------------------------------------------------------------
        // 7. PUBLISH_ERROR + file → downloadable for retry; state preserved.
        // ------------------------------------------------------------------
        ArrExport errE = prepareExport(fund, active, ArrExport.State.PUBLISH_ERROR, 35L, true);
        publicationApi.publicationDownloadPublication(errE.getExportId());
        assertEquals(ArrExport.State.PUBLISH_ERROR, exportRepository.findById(errE.getExportId()).orElseThrow().getState());

        // ------------------------------------------------------------------
        // 8. Inactive type → 403, even for a previously-downloadable id.
        // ------------------------------------------------------------------
        deactivateType(active);
        HttpClientErrorException forbidden = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationDownloadPublication(prepE.getExportId()));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }    

    /**
     * Covers {@code POST /publications/status/{id}}:
     *
     *   • OK on FETCHED → PUBLISHED; ERROR on FETCHED → PUBLISH_ERROR,
     *   • idempotent replay (same payload) is a 200 no-op,
     *   • OK with a different publishedAt on PUBLISHED → 409,
     *   • ERROR on PUBLISHED → 409 (already-successful publication not rolled back),
     *   • ERROR on PUBLISH_ERROR with different details → overwrites (recovery path),
     *   • OK on PUBLISH_ERROR → recovery to PUBLISHED,
     *   • OK on PREPARED (no download yet) → 409,
     *   • internal-only states (NEW / PREPARE_ERROR / INVALIDATED) → 404,
     *   • unknown id → 404.
     *
     * UTC timestamps truncated to millis avoid roundtrip drift when comparing
     * stored publishedAt / errorAt to the value sent over the wire.
     */
    @Test
    public void publicationReportPublicationStatusTest() {
        TestContext ctx = setupUserAndFund();
        ArrExportType active = createTypeAndFetch(ctx, "TST_ST_ACTIVE");
        Integer fund = ctx.fundId;

        OffsetDateTime t1 = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
        OffsetDateTime t2 = t1.plusMinutes(5);

        // ------------------------------------------------------------------
        // 1. Unknown id → 404.
        // ------------------------------------------------------------------
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationReportPublicationStatus(999_999, reportOk(t1)));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());

        // ------------------------------------------------------------------
        // 2. Internal-only states (NEW / PREPARE_ERROR / INVALIDATED) → 404,
        //    regardless of what the report says.
        // ------------------------------------------------------------------
        ArrExport newE = prepareExport(fund, active, ArrExport.State.NEW,           null, false);
        ArrExport peE  = prepareExport(fund, active, ArrExport.State.PREPARE_ERROR, null, false);
        ArrExport invE = prepareExport(fund, active, ArrExport.State.INVALIDATED,   1L,   false);
        for (Integer hidden : List.of(newE.getExportId(), peE.getExportId(), invE.getExportId())) {
            HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                    () -> publicationApi.publicationReportPublicationStatus(hidden, reportOk(t1)));
            assertEquals("publication id " + hidden + " must be hidden from the public API", 
            		HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        // ------------------------------------------------------------------
        // 3. Reporting before any download (PREPARED) is a protocol violation → 409.
        // ------------------------------------------------------------------
        ArrExport prepE = prepareExport(fund, active, ArrExport.State.PREPARED, 10L, true);
        HttpClientErrorException tooEarly = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationReportPublicationStatus(prepE.getExportId(), reportOk(t1)));
        assertEquals(HttpStatus.CONFLICT, tooEarly.getStatusCode());

        // ------------------------------------------------------------------
        // 4. Happy path OK: FETCHED → PUBLISHED.
        // ------------------------------------------------------------------
        ArrExport okE = prepareExport(fund, active, ArrExport.State.FETCHED, 20L, true);
        publicationApi.publicationReportPublicationStatus(okE.getExportId(), reportOk(t1));

        ArrExport okStored = exportRepository.findById(okE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISHED, okStored.getState());
        assertEquals(t1, okStored.getPublishedAt());
        assertNull(okStored.getErrorMessage());
        assertNull(okStored.getErrorAt());

        // ------------------------------------------------------------------
        // 5. Idempotent OK replay (same publishedAt) → 200, nothing changes.
        // ------------------------------------------------------------------
        publicationApi.publicationReportPublicationStatus(okE.getExportId(), reportOk(t1));
        ArrExport okReplayed = exportRepository.findById(okE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISHED, okReplayed.getState());
        assertEquals(t1, okReplayed.getPublishedAt());

        // ------------------------------------------------------------------
        // 6. OK on PUBLISHED with a different publishedAt → 409, stored
        //    state preserved untouched.
        // ------------------------------------------------------------------
        HttpClientErrorException okConflict = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationReportPublicationStatus(okE.getExportId(), reportOk(t2)));
        assertEquals(HttpStatus.CONFLICT, okConflict.getStatusCode());
        ArrExport okAfterConflict = exportRepository.findById(okE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISHED, okAfterConflict.getState());
        assertEquals(t1, okAfterConflict.getPublishedAt());

        // ------------------------------------------------------------------
        // 7. ERROR on PUBLISHED → 409, must not roll back a successful
        //    publication.
        // ------------------------------------------------------------------
        HttpClientErrorException lateError = assertThrows(HttpClientErrorException.class,
                () -> publicationApi.publicationReportPublicationStatus(okE.getExportId(),
                        reportError(t2, "late failure")));
        assertEquals(HttpStatus.CONFLICT, lateError.getStatusCode());
        ArrExport okAfterLateError = exportRepository.findById(okE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISHED, okAfterLateError.getState());
        assertNull(okAfterLateError.getErrorMessage());

        // ------------------------------------------------------------------
        // 8. Happy path ERROR: FETCHED → PUBLISH_ERROR.
        // ------------------------------------------------------------------
        ArrExport errE = prepareExport(fund, active, ArrExport.State.FETCHED, 30L, true);
        publicationApi.publicationReportPublicationStatus(errE.getExportId(), reportError(t1, "boom"));

        ArrExport errStored = exportRepository.findById(errE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISH_ERROR, errStored.getState());
        assertEquals(t1, errStored.getErrorAt());
        assertEquals("boom", errStored.getErrorMessage());

        // ------------------------------------------------------------------
        // 9. Idempotent ERROR replay (same errorAt + same message) → no-op.
        // ------------------------------------------------------------------
        publicationApi.publicationReportPublicationStatus(errE.getExportId(), reportError(t1, "boom"));
        ArrExport errReplayed = exportRepository.findById(errE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISH_ERROR, errReplayed.getState());
        assertEquals(t1, errReplayed.getErrorAt());
        assertEquals("boom", errReplayed.getErrorMessage());

        // ------------------------------------------------------------------
        // 10. ERROR on PUBLISH_ERROR with different details → overwrites
        //     (spec: "may be reported again").
        // ------------------------------------------------------------------
        publicationApi.publicationReportPublicationStatus(errE.getExportId(), reportError(t2, "second"));
        ArrExport errOverwritten = exportRepository.findById(errE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISH_ERROR, errOverwritten.getState());
        assertEquals(t2, errOverwritten.getErrorAt());
        assertEquals("second", errOverwritten.getErrorMessage());

        // ------------------------------------------------------------------
        // 11. Recovery: PUBLISH_ERROR → PUBLISHED via OK; error fields cleared.
        // ------------------------------------------------------------------
        publicationApi.publicationReportPublicationStatus(errE.getExportId(), reportOk(t2));
        ArrExport recovered = exportRepository.findById(errE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISHED, recovered.getState());
        assertEquals(t2, recovered.getPublishedAt());
        assertNull(recovered.getErrorMessage());
        assertNull(recovered.getErrorAt());

        // ------------------------------------------------------------------
        // 12. ERROR with null message is accepted (spec: errorMessage is optional).
        // ------------------------------------------------------------------
        ArrExport nullMsgE = prepareExport(fund, active, ArrExport.State.FETCHED, 40L, true);
        publicationApi.publicationReportPublicationStatus(nullMsgE.getExportId(), reportError(t1, null));
        ArrExport nullMsgStored = exportRepository.findById(nullMsgE.getExportId()).orElseThrow();
        assertEquals(ArrExport.State.PUBLISH_ERROR, nullMsgStored.getState());
        assertNull(nullMsgStored.getErrorMessage());
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Status-report helpers
    // ----------------------------------------------------------------------

    private PublicationStatusReport reportOk(final OffsetDateTime at) {
        PublicationStatusReport r = new PublicationStatusReport();
        r.setStatus(PublicationReportStatus.OK);
        r.setPublishedAt(at);
        return r;
    }

    private PublicationStatusReport reportError(final OffsetDateTime at, final String message) {
        PublicationStatusReport r = new PublicationStatusReport();
        r.setStatus(PublicationReportStatus.ERROR);
        r.setPublishedAt(at);
        r.setErrorMessage(message);
        return r;
    }    

    /**
     * Non-admin user with FUND_ADMIN + one fund the user owns. Admin login
     * leaves {@code UserService.getLoggedUser()} null, which would prevent
     * {@code arr_export.user_id} from being filled in by {@link #prepareExport}.
     */
    private TestContext setupUserAndFund() {
        // clean work/dms folder
        try {
            java.io.File dmsDir = resourcePathResolver.getDmsDir().toFile();
            if (dmsDir.exists()) {
                FileUtils.cleanDirectory(dmsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clean DMS dir", e);
        }
        ApAccessPointVO ap = findRecord(null, null, null, null, null).get(0);
        UsrUserVO user = createUser(ap.getId(), "publication-user", "publication-pass");
        UsrPermissionVO faPermission = new UsrPermissionVO();
        faPermission.setPermission(UsrPermission.Permission.FUND_ADMIN);
        UsrPermissionVO fePermission = new UsrPermissionVO();
        fePermission.setPermission(UsrPermission.Permission.FUND_EXPORT_ALL);
        addUserPermission(user.getId(), List.of(faPermission, fePermission));
        login("publication-user", "publication-pass");

        Fund fund = createFund("publication-fund", "publication-fund-code");
        return new TestContext(user.getId(), fund.getId());
    }

    /**
     * Creates a publication type via the admin endpoint and reloads its
     * server-side entity so the test can pass it to {@link #prepareExport}.
     * Switches back to the FUND_ADMIN user afterwards.
     */
    private ArrExportType createTypeAndFetch(final TestContext ctx, final String code) {
        loginAsAdmin();
        try {
            PublicationType vo = new PublicationType();
            vo.setCode(code);
            vo.setName(code);
            vo.setActive(true);
            vo.setRetentionCount(5);
            vo.setAllowPermExport(true);
            vo.setAllowPermPublication(false);
            vo.setConnectionType(ConnectionType.TEST);
            PublicationType created = publicationIntApi.publicationTypeAdminCreatePublicationType(vo);
            return exportTypeRepository.findById(created.getId()).orElseThrow();
        } finally {
            login("publication-user", "publication-pass");
        }
    }

    private void deactivateType(final ArrExportType type) {
        type.setActive(false);
        exportTypeRepository.save(type);
    }

    /**
     * Persists an {@code arr_export} directly via repositories, bypassing
     * the async generator. The public-API tests need precise control over
     * state, {@code exportSeq} and file presence — none of which the create
     * endpoint exposes.
     */
    private ArrExport prepareExport(final Integer fundId,
                                    final ArrExportType type,
                                    final ArrExport.State state,
                                    final Long exportSeq,
                                    final boolean withFile) {
        ArrFundVersion fv = fundVersionRepository.findByFundIdAndLockChangeIsNull(fundId);
        UsrUser owner = userService.findByUsername("publication-user");

        ArrExport export = new ArrExport();
        export.setExportType(type);
        export.setFundVersion(fv);
        export.setExportFilter(type.getExportFilter());
        export.setState(state);
        export.setCreatedAt(OffsetDateTime.now());
        export.setUser(owner);
        export.setExportSeq(exportSeq);

        if (withFile) {
            DmsFile file = new DmsFile();
            file.setName("publication-" + state.name() + "-" + exportSeq + ".xml");
            file.setFileName(file.getName());
            file.setMimeType("application/xml");
            byte[] xml = ("<doc state=\"" + state + "\"/>").getBytes(StandardCharsets.UTF_8);
            file.setFileSize(xml.length);
            try {
				dmsService.createFile(file, new ByteArrayInputStream(xml));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
            export.setFile(file);
        }
        return exportRepository.save(export);
    }

    private static final class TestContext {
        final Integer userId;
        final Integer fundId;

        TestContext(final Integer userId, final Integer fundId) {
            this.userId = userId;
            this.fundId = fundId;
        }
    }
}