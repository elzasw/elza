package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.test.ApiClient;
import cz.tacr.elza.test.controller.vo.Institution;
import cz.tacr.elza.test.controller.vo.InstitutionType;

public class InstitutionControllerTest extends AbstractControllerTest {

    @Test
    public void getAllTest() {
        List<Institution> list = institutionApi.instGetAll();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Test data should contain at least one institution");

        Institution any = list.get(0);
        assertNotNull(any.getId());
        assertNotNull(any.getInternalCode());
        assertNotNull(any.getAccessPointId());
    }

    @Test
    public void getTypesTest() {
        List<InstitutionType> types = institutionApi.instGetTypes();
        assertNotNull(types);
        assertFalse(types.isEmpty(), "At least the DEFAULT type must be seeded");

        InstitutionType any = types.get(0);
        assertNotNull(any.getId());
        assertNotNull(any.getCode());
        assertNotNull(any.getName());
    }

    @Test
    public void getByIdTest() {
        Institution seed = institutionApi.instGetAll().get(0);

        Institution byId = institutionApi.instGetById(seed.getId().toString());
        assertEquals(seed.getId(), byId.getId());
        assertEquals(seed.getInternalCode(), byId.getInternalCode());

        Institution byCode = institutionApi.instGetById(seed.getInternalCode());
        assertEquals(seed.getId(), byCode.getId());
    }

    @Test
    public void getByIdNotFoundTest() {
        HttpClientErrorException notFound = assertThrows(HttpClientErrorException.class,
                () -> institutionApi.instGetById("no-such-code-xyz"));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
    }

    @Test
    public void createUpdateDeleteTest() {
        Institution seed = institutionApi.instGetAll().get(0);
        Integer someTypeId = institutionApi.instGetTypes().get(0).getId();

        // CREATE
        Institution toCreate = new Institution()
                .id(0)
                .internalCode("test-inst-code")
                .accessPointId(seed.getAccessPointId())
                .institutionTypeId(someTypeId)
                .shortName("short");

        Institution created = institutionApi.instCreate(toCreate);
        assertNotNull(created.getId());
        assertEquals("test-inst-code", created.getInternalCode());
        assertEquals("short", created.getShortName());
        assertEquals(someTypeId, created.getInstitutionTypeId());
        assertEquals(seed.getAccessPointId(), created.getAccessPointId());

        // UPDATE
        created.setInternalCode("test-inst-code-2");
        created.setShortName("short-2");
        Institution updated = institutionApi.instUpdate(created.getId(), created);
        assertEquals("test-inst-code-2", updated.getInternalCode());
        assertEquals("short-2", updated.getShortName());
        assertEquals(created.getAccessPointId(), updated.getAccessPointId());

        // DELETE
        institutionApi.instDelete(created.getId());

        HttpClientErrorException afterDelete = assertThrows(HttpClientErrorException.class,
                () -> institutionApi.instGetById(created.getId().toString()));
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
    }

    @Test
    public void anonymousGetAllIsUnauthorizedTest() {
        ApiClient anon = new ApiClient();
        anon.setBasePath(institutionApi.getApiClient().getBasePath());
        cz.tacr.elza.test.controller.InstitutionApi anonApi = new cz.tacr.elza.test.controller.InstitutionApi(anon);

        HttpClientErrorException denied = assertThrows(HttpClientErrorException.class, anonApi::instGetAll);
        assertEquals(HttpStatus.UNAUTHORIZED, denied.getStatusCode());
    }
}