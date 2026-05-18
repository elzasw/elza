package cz.tacr.elza.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

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

        // Unknown fund id → 404.
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class, 
        		() -> publicationIntApi.fundPublicationListFundPublications(999_999, null, 0, 50));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());    	
    }

    @Test
    public void fundPublicationCreateFundPublicationTest() {
        TestContext ctx = setupUserAndFund();
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

        // outstanding NEW publication for the same fund+type → 409.
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

        CreatePublication createBody = new CreatePublication();
        createBody.setPublicationTypeId(source.getId());
        PublicationDetail original = publicationIntApi.fundPublicationCreateFundPublication(ctx.fundId, createBody);

        CopyPublication copyBody = new CopyPublication();
        copyBody.setTargetPublicationTypeId(targetCompat.getId());
        PublicationDetail copy = publicationIntApi.fundPublicationCopyFundPublication(ctx.fundId, original.getId(), copyBody);
        assertNotNull(copy.getId());
        assertEquals(targetCompat.getId(), copy.getTypeId());
        assertEquals(PublicationStateInternal.NEW, copy.getState());

        // Incompatible filter setting (target uses a filter, source does not) → 409.
        CopyPublication incompatible = new CopyPublication();
        incompatible.setTargetPublicationTypeId(targetWithFilter.getId());
        HttpClientErrorException conflict = assertThrows(HttpClientErrorException.class,
                () -> publicationIntApi.fundPublicationCopyFundPublication(ctx.fundId, original.getId(), incompatible));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /**
     * Provisions a non-admin user with FUND_ADMIN and a fund owned by that user.
     * Admin authentication leaves UserService.getLoggedUser() null, which would
     * prevent arr_export.user_id from being filled in.
     */
    private TestContext setupUserAndFund() {
        ApAccessPointVO ap = findRecord(null, null, null, null, null).get(0);
        UsrUserVO user = createUser(ap.getId(), "publication-user", "publication-pass");
        UsrPermissionVO permission = new UsrPermissionVO();
        permission.setPermission(UsrPermission.Permission.FUND_ADMIN);
        addUserPermission(user.getId(), List.of(permission));
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

		public Integer getUserId() {
			return userId;
		}

		public Integer getFundId() {
			return fundId;
		}
    }
}