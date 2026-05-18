package cz.tacr.elza.controller;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.test.controller.vo.ConnectionType;
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

        // duplicate code  → 409
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
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminUpdatePublicationType(updated.getId(), badUdate));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getResponseHeaders());    

        // Updating a non-existent id → 404
        ex = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminUpdatePublicationType(999_999, update));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getResponseHeaders());
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
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> publicationIntApi.publicationTypeAdminDeletePublicationType(999_999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertNotNull(ex.getResponseHeaders());    
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

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
}