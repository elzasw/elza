package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.test.controller.vo.ConnectionType;
import cz.tacr.elza.test.controller.vo.CopyPublication;
import cz.tacr.elza.test.controller.vo.CreatePublication;
import cz.tacr.elza.test.controller.vo.Fund;
import cz.tacr.elza.test.controller.vo.PublicationDetail;
import cz.tacr.elza.test.controller.vo.PublicationList;
import cz.tacr.elza.test.controller.vo.PublicationStateInternal;
import cz.tacr.elza.test.controller.vo.PublicationType;

/**
 * Tests for {@link PublicationInternalController} — exercises the admin CRUD
 * endpoints for {@code arr_export_type}. Uses the default per-method test
 * lifecycle, so {@code AbstractTest.setUp} wipes the DB between tests; each
 * test starts from an empty publication-type list.
 */
public class PublicationControllerTest extends AbstractControllerTest {

    @Test
    public void publicationTypeAdminListPublicationTypesTest() {
    	List<PublicationType> publications = publicationIntApi.publicationTypeAdminListPublicationTypes();
        assertTrue(publications.isEmpty(), "list should be empty on a fresh DB");

        publicationIntApi.publicationTypeAdminCreatePublicationType(buildPublicationType("TST_LIST_1", "Test list 1"));
        publicationIntApi.publicationTypeAdminCreatePublicationType(buildPublicationType("TST_LIST_2", "Test list 2"));

        publications = publicationIntApi.publicationTypeAdminListPublicationTypes();
        assertEquals(2, publications.size());
        List<String> codes = publications.stream().map(PublicationType::getCode).collect(Collectors.toList());
        assertTrue(codes.contains("TST_LIST_1"));
        assertTrue(codes.contains("TST_LIST_2"));
    }

    @Test
    public void publicationTypeAdminCreatePublicationTypeTest() {
        PublicationType publicationType = buildPublicationType("TST_CREATE", "Test create");
        publicationType.setRetentionCount(3);
        publicationType.setActive(false);
        publicationType.setAllowPermExport(true);
        publicationType.setAllowPermPublication(true);

        PublicationType created = publicationIntApi.publicationTypeAdminCreatePublicationType(publicationType);
        assertNotNull(created.getId());
        assertEquals("TST_CREATE", created.getCode());
        assertEquals("Test create", created.getName());
        assertEquals(3, created.getRetentionCount());
        assertEquals(false, created.getActive());
        assertEquals(true, created.getAllowPermExport());
        assertEquals(true, created.getAllowPermPublication());

        // Duplicate code  → 409
        PublicationType badPublicationType = buildPublicationType("TST_CREATE", "Different name");
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminCreatePublicationType(badPublicationType));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getResponseHeaders());    
    }

    @Test
    public void publicationTypeAdminUpdatePublicationTypeTest() {
        PublicationType createdA = publicationIntApi.publicationTypeAdminCreatePublicationType(buildPublicationType("TST_UPDATE_A", "Original"));
        publicationIntApi.publicationTypeAdminCreatePublicationType(buildPublicationType("TST_UPDATE_C", "Original")); // for conflict update

        PublicationType update = buildPublicationType("TST_UPDATE_B", "Renamed");
        update.setRetentionCount(7);
        update.setActive(false);
        PublicationType updated = publicationIntApi.publicationTypeAdminUpdatePublicationType(createdA.getId(), update);

        assertEquals(createdA.getId(), updated.getId());
        assertEquals("TST_UPDATE_B", updated.getCode());
        assertEquals("Renamed", updated.getName());
        assertEquals(7, updated.getRetentionCount());
        assertEquals(false, updated.getActive());

        // Updating to a code already taken by another type → 409
        PublicationType badUdate = buildPublicationType("TST_UPDATE_C", "Other");
        HttpClientErrorException conflict = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminUpdatePublicationType(updated.getId(), badUdate));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertNotNull(conflict.getResponseHeaders());    

        // Updating a non-existent id → 404
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminUpdatePublicationType(999_999, update));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertNotNull(notFound.getResponseHeaders());
    }

    @Test
    public void publicationTypeAdminDeletePublicationTypeTest() {
        PublicationType created = publicationIntApi.publicationTypeAdminCreatePublicationType(buildPublicationType("TST_DELETE", "To remove"));
        List<PublicationType> publications = publicationIntApi.publicationTypeAdminListPublicationTypes();
        assertEquals(1, publications.size());

        publicationIntApi.publicationTypeAdminDeletePublicationType(created.getId());
        publications = publicationIntApi.publicationTypeAdminListPublicationTypes();
        assertTrue(publications.isEmpty());

        // Deleting unknown id → 404
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminDeletePublicationType(999_999));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertNotNull(notFound.getResponseHeaders());
    }

    @Test
    public void fundPublicationListFundPublicationsTest() {
        TestContext ctx = setupUserAndFund();
        CreatePublication createBody = new CreatePublication();
        PublicationType typeA = createTypeAs(ctx, "TST_FUND_LIST_A", "List A", null);
        PublicationType typeB = createTypeAs(ctx, "TST_FUND_LIST_B", "List B", null);

        createBody.setPublicationTypeId(typeA.getId());
        PublicationDetail first = publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody);
        // Invalidate the first one so a subsequent create against typeA would be allowed
        // (NEW → INVALIDATED clears the outstanding-publication block).
        publicationIntApi.fundPublicationInvalidateFundPublication(ctx.fundId, first.getId());
        createBody.setPublicationTypeId(typeB.getId());
        PublicationDetail second = publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody);

        PublicationList all = publicationIntApi.fundPublicationListFundPublications(ctx.fundId, null, 0, 50);
        assertEquals(2, all.getTotalCount());
        // Newest first.
        assertEquals(second.getId(), all.getItems().get(0).getId());

        PublicationList filtered = publicationIntApi.fundPublicationListFundPublications(ctx.fundId, typeB.getId(), 0, 50);
        assertEquals(1, filtered.getTotalCount());
        assertEquals(typeB.getId(), filtered.getItems().get(0).getTypeId());

        // Unknown fund id → empty page. The list endpoint deliberately does
        // not require fund existence (a locked-or-missing fund still returns
        // a meaningful — empty — history page, never 404).
        PublicationList unknown = publicationIntApi.fundPublicationListFundPublications(999_999, null, 0, 50);
        assertEquals(0, unknown.getTotalCount());
        assertTrue(unknown.getItems().isEmpty());
    }

    @Test
    public void fundPublicationCreateFundPublicationTest() {
        TestContext ctx = setupUserAndFund();
        // Pause the async worker so the publication stays in NEW state and
        // the "outstanding NEW" duplicate-block assertion below is testable.
        asyncRequestService.stop();

        CreatePublication createBody = new CreatePublication();
        PublicationType type = createTypeAs(ctx, "TST_FUND_CREATE", "Create test", null);

        createBody.setPublicationTypeId(type.getId());
        PublicationDetail detail = publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody);
        assertNotNull(detail.getId());
        assertEquals(type.getId(), detail.getTypeId());
        assertEquals(PublicationStateInternal.NEW, detail.getState());
        assertEquals(ctx.userId, detail.getCreatedBy().getUserId());
        assertNotNull(detail.getCreatedAt());
        assertFalse(detail.getHasDownloadableFile());
        assertNull(detail.getPreparedAt());
        assertNull(detail.getInvalidatedAt());

        // Only NEW blocks — second create against the same fund + type → 409.
        // PREPARED would not block, so the test must keep the worker paused.
        HttpClientErrorException duplicate = assertThrows(HttpClientErrorException.class,
                () -> publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        // unknown publication type → 404.
        createBody.setPublicationTypeId(999_999);
        HttpClientErrorException unknownType = assertThrows(HttpClientErrorException.class,
                () -> publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody));
        assertEquals(HttpStatus.NOT_FOUND, unknownType.getStatusCode());
    }

    @Test
    public void fundPublicationCopyFundPublicationTest() {
        TestContext ctx = setupUserAndFund();
        PublicationType source = createTypeAs(ctx, "TST_FUND_COPY_SRC", "Copy source", null);
        PublicationType targetCompat = createTypeAs(ctx, "TST_FUND_COPY_DST", "Copy target", null);
        PublicationType targetWithFilter = createTypeAs(ctx, "TST_FUND_COPY_FILTERED", "Copy filtered target", "SRD_TEST_EXPORT_FILTER");

        // Source must be PREPARED (have a file) before it can be copied —
        // copy re-links the existing dms_file, it does not regenerate.
        PublicationDetail original = publishAndWait(ctx.fundId, source.getId());
        assertEquals(PublicationStateInternal.PREPARED, original.getState());
        assertTrue(original.getHasDownloadableFile());

        CopyPublication copyBody = new CopyPublication();
        copyBody.setTargetPublicationTypeId(targetCompat.getId());
        PublicationDetail copy = publicationIntApi.fundPublicationCopyFundPublication(ctx.fundId, original.getId(), copyBody);
        assertNotNull(copy.getId());
        assertEquals(targetCompat.getId(), copy.getTypeId());
        // Copy is immediately PREPARED — no async work, file is reused.
        assertEquals(PublicationStateInternal.PREPARED, copy.getState());
        assertTrue(copy.getHasDownloadableFile());
        // preparedAt is data-lifecycle: inherited from source (same file).
        assertEquals(original.getPreparedAt(), copy.getPreparedAt());

        // Incompatible filter setting (target uses a filter, source does not) → 409.
        CopyPublication incompatible = new CopyPublication();
        incompatible.setTargetPublicationTypeId(targetWithFilter.getId());
        HttpClientErrorException conflict = assertThrows(HttpClientErrorException.class,
                () -> publicationIntApi.fundPublicationCopyFundPublication(ctx.fundId, original.getId(), incompatible));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }

    /**
     * Exercises the full retention + copy + invalidate lifecycle:
     *
     * <ol>
     *   <li>Three publishes of type {@code A} (retention=2) — the oldest loses
     *       its file when the third is generated.</li>
     *   <li>Copying a swept publication fails (no file to re-link).</li>
     *   <li>Copying a still-retained publication to type {@code B} creates an
     *       immediately PREPARED sibling that shares the file.</li>
     *   <li>Invalidating the source keeps the file alive (the copy still
     *       references it).</li>
     *   <li>Invalidating the copy drops the last reference; the file is
     *       physically deleted.</li>
     * </ol>
     */
    @Test
    public void publicationRetentionAndCopyLifecycleTest() {
        TestContext ctx = setupUserAndFund();

        // Two compatible types with retention=2 each.
        PublicationType typeA = createTypeAs(ctx, "TST_LIFE_A", "Lifecycle A", null, 2);
        PublicationType typeB = createTypeAs(ctx, "TST_LIFE_B", "Lifecycle B", null, 2);

        // Three sequential publishes of typeA. The "outstanding" guard only
        // blocks NEW, so a previously-PREPARED publication does not stop
        // the next create — and each waitForWorkers() drains the queue
        // before the next publish runs.
        PublicationDetail a1 = publishAndWait(ctx.fundId, typeA.getId());
        PublicationDetail a2 = publishAndWait(ctx.fundId, typeA.getId());
        PublicationDetail a3 = publishAndWait(ctx.fundId, typeA.getId());

        // After the third publish, retention=2 sweeps the oldest (a1).
        // Rows stay; only files are removed.
        Map<Integer, PublicationDetail> stateA = byId(ctx.fundId, typeA.getId());
        assertFalse(stateA.get(a1.getId()).getHasDownloadableFile(),
                "oldest publication should have lost its file");
        assertTrue(stateA.get(a2.getId()).getHasDownloadableFile(),
                "publication within retention window keeps its file");
        assertTrue(stateA.get(a3.getId()).getHasDownloadableFile(),
                "newest publication keeps its file");

        // Copying a swept publication fails — no file to re-link.
        CopyPublication copyToB = new CopyPublication();
        copyToB.setTargetPublicationTypeId(typeB.getId());
        HttpClientErrorException sweptCopy = assertThrows(HttpClientErrorException.class,
                () -> publicationIntApi.fundPublicationCopyFundPublication(ctx.fundId, a1.getId(), copyToB));
        assertEquals(HttpStatus.NOT_FOUND, sweptCopy.getStatusCode());

        // Copy a still-retained publication to typeB. New row is immediately
        // PREPARED and shares the dms_file with a2.
        PublicationDetail copy = publicationIntApi.fundPublicationCopyFundPublication(ctx.fundId, a2.getId(), copyToB);
        assertEquals(typeB.getId(), copy.getTypeId());
        assertEquals(PublicationStateInternal.PREPARED, copy.getState());
        assertTrue(copy.getHasDownloadableFile());

        // Invalidate a2. The dms_file is still referenced by the copy,
        // so the file is kept and only a2's link is nulled.
        publicationIntApi.fundPublicationInvalidateFundPublication(ctx.fundId, a2.getId());
        PublicationDetail a2Reloaded = findById(ctx.fundId, a2.getId());
        assertEquals(PublicationStateInternal.INVALIDATED, a2Reloaded.getState());
        assertFalse(a2Reloaded.getHasDownloadableFile());
        assertNotNull(a2Reloaded.getInvalidatedAt());
        // The copy is unaffected — it can still be downloaded.
        PublicationDetail copyReloaded = findById(ctx.fundId, copy.getId());
        assertTrue(copyReloaded.getHasDownloadableFile(),
                "copy retains its file when the source is invalidated (file is shared)");

        // Invalidate the copy. No other export references the file now,
        // so the dms_file is physically deleted.
        publicationIntApi.fundPublicationInvalidateFundPublication(ctx.fundId, copy.getId());
        PublicationDetail copyFinal = findById(ctx.fundId, copy.getId());
        assertEquals(PublicationStateInternal.INVALIDATED, copyFinal.getState());
        assertFalse(copyFinal.getHasDownloadableFile());
    }

    /**
     * Dynamic type-flag gate: a user with only {@code FUND_EXPORT_ALL} cannot
     * publish into a type configured as {@code allowPermExport=false,
     * allowPermPublication=true} — the gate honours the type's flags rather
     * than just the caller's permissions. {@code FUND_ADMIN} would bypass
     * this; we deliberately don't grant it on the weak user.
     */
    @Test
    public void fundPublicationCreateDeniedWhenTypeFlagsMismatchTest() {
        TestContext ctx = setupUserAndFund();
        asyncRequestService.stop();

        // Type configured as publish-only.
        PublicationType publishOnly = createTypeAs(ctx, "TST_DENIED_TYPE", "Publish-only", null);
        publishOnly.setAllowPermExport(false);
        publishOnly.setAllowPermPublication(true);
        loginAsAdmin();
        publicationIntApi.publicationTypeAdminUpdatePublicationType(publishOnly.getId(), publishOnly);

        // Second user with only FUND_EXPORT_ALL — no FUND_PUBLISH*, no FUND_ADMIN, no ADMIN.
        ApAccessPointVO ap = findRecord(null, null, null, null, null).get(0);
        UsrUserVO weak = createUser(ap.getId(), "fund-export-only", "fund-export-only-pass");
        UsrPermissionVO ferPermission = new UsrPermissionVO();
        ferPermission.setPermission(UsrPermission.Permission.FUND_EXPORT_ALL);
        addUserPermission(weak.getId(), List.of(ferPermission));
        login("fund-export-only", "fund-export-only-pass");

        CreatePublication createBody = new CreatePublication();
        createBody.setPublicationTypeId(publishOnly.getId());
        HttpClientErrorException denied = assertThrows(HttpClientErrorException.class,
                () -> publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody));
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /**
     * Provisions a non-admin user with FUND_ADMIN and a fund owned by that user.
     * Admin authentication leaves UserService.getLoggedUser() null, which would
     * prevent arr_export.user_id from being filled in.
     *
     * Also wipes the DMS work directory because dms_file ids are generated by
     * the hibernate-sequences table (reset between tests) but the physical
     * files on disk are not; without a wipe the second run hits
     * "Nelze soubor již existuje".
     */
    private TestContext setupUserAndFund() {
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
        // FUND_RD_ALL is required by the publication list endpoint.
        UsrPermissionVO frdPermission = new UsrPermissionVO();
        frdPermission.setPermission(UsrPermission.Permission.FUND_RD_ALL);
        addUserPermission(user.getId(), List.of(faPermission, fePermission, frdPermission));
        login("publication-user", "publication-pass");

        Fund fund = createFund("publication-fund", "publication-fund-code");
        return new TestContext(user.getId(), fund.getId());
    }

	private PublicationType createTypeAs(final TestContext ctx, final String code, final String name, final String exportFilterCode) {
		// Type creation requires admin; switch back after so fund-scoped calls
		// run as the FUND_ADMIN user provisioned in setupUserAndFund().
		loginAsAdmin();
		try {
			PublicationType vo = buildPublicationType(code, name);
			vo.setExportFilterCode(exportFilterCode);
			return publicationIntApi.publicationTypeAdminCreatePublicationType(vo);
		} finally {
			login("publication-user", "publication-pass");
		}
	}

	private PublicationType createTypeAs(final TestContext ctx, final String code, final String name,
			final String exportFilterCode, final int retentionCount) {
		loginAsAdmin();
		try {
			PublicationType vo = buildPublicationType(code, name);
			vo.setExportFilterCode(exportFilterCode);
			vo.setRetentionCount(retentionCount);
			return publicationIntApi.publicationTypeAdminCreatePublicationType(vo);
		} finally {
			login("publication-user", "publication-pass");
		}
	}

	/**
	 * Trigger a publish for the fund + type, wait for the async generator
	 * (and any subsequent retention sweep) to finish, and return the freshly
	 * reloaded publication detail.
	 */
	private PublicationDetail publishAndWait(final Integer fundId, final Integer typeId) {
		CreatePublication body = new CreatePublication();
		body.setPublicationTypeId(typeId);
		PublicationDetail created = publicationIntApi.fundPublicationCreateFundPublication(fundId, body);
		helperTestService.waitForWorkers();
		return findById(fundId, created.getId());
	}

	/**
	 * Re-fetch a publication's current state via the list endpoint (no
	 * single-record GET exists on the internal API).
	 */
	private PublicationDetail findById(final Integer fundId, final Integer publicationId) {
		PublicationList list = publicationIntApi.fundPublicationListFundPublications(fundId, null, 0, 200);
		return list.getItems().stream()
				.filter(item -> item.getId().equals(publicationId))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Publication not found: " + publicationId));
	}

	/**
	 * Index publications of a fund + type by ID for assertions that don't
	 * care about ordering.
	 */
	private Map<Integer, PublicationDetail> byId(final Integer fundId, final Integer typeId) {
		PublicationList list = publicationIntApi.fundPublicationListFundPublications(fundId, typeId, 0, 200);
		return list.getItems().stream()
				.collect(Collectors.toMap(PublicationDetail::getId, p -> p));
	}

    private PublicationType buildPublicationType(final String code, final String name) {
        PublicationType ptVO = new PublicationType();
        ptVO.setCode(code);
        ptVO.setName(name);
        ptVO.setActive(true);
        ptVO.setRetentionCount(5);
        ptVO.setAllowPermExport(true);
        ptVO.setAllowPermPublication(false);
        ptVO.setConnectionType(ConnectionType.TEST);
        return ptVO;
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