package cz.tacr.elza.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.security.apikey.ApiKeyProperties;
import cz.tacr.elza.test.ApiClient;
import cz.tacr.elza.test.controller.AdminApi;
import cz.tacr.elza.test.controller.vo.AdminInfo;
import cz.tacr.elza.test.controller.vo.ApiKeyCreateRequest;
import cz.tacr.elza.test.controller.vo.ApiKeyCreated;
import io.restassured.RestAssured;

/**
 * End-to-end for personal API keys: a user creates one, calls a protected endpoint with the
 * {@code X-API-Key} header, revokes the key, and the same call is then rejected.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiKeyControllerTest extends AbstractControllerTest {

    private static final String USR_NAME = "apikey-user";
    private static final String USR_PSWD = "apikey-user";

    @BeforeAll
    public void initOnce() throws Exception {
        super.setUp();
    }

    @AfterAll
    public void cleanupOnce() {
        super.tearDown();
    }

    @Override
    @BeforeEach
    public void setUp() {
        // no-op: setup runs once in @BeforeAll
    }

    @Override
    @AfterEach
    public void tearDown() {
        // no-op: cleanup runs once in @AfterAll
    }

    @Test
    public void keyLifecycle() {
        // A real user (not the built-in admin) is needed — key creation requires a usr_user row
        List<ApAccessPointVO> records = findRecord(null, null, null, null, null);
        ApAccessPointVO apUser = records.get(1);
        UsrUserVO owner = createUser(apUser.getId(), USR_NAME, USR_PSWD);
        UsrPermissionVO adminPerm = new UsrPermissionVO();
        adminPerm.setPermission(Permission.ADMIN);
        addUserPermission(owner.getId(), Arrays.asList(adminPerm));
        login(USR_NAME, USR_PSWD);

        // 1) Create a key via the session-authenticated endpoint
        ApiKeyCreateRequest req = new ApiKeyCreateRequest();
        req.setName("integration test key");
        ApiKeyCreated created = userApi.apiKeysCreate(req);
        String token = created.getToken();
        Integer keyPk = created.getApiKey().getId();
        assertNotNull(token);
        assertTrue(token.startsWith("elza_"), "Token must carry the elza_ prefix, got " + token);

        // 2) Call a protected endpoint authenticated by the key alone → 200. A fresh ApiClient
        //    carries no session cookies so the request is routed through the stateless
        //    API-key security chain
        AdminApi adminByKey = new AdminApi(newApiKeyClient(token));
        AdminInfo info = adminByKey.adminInfo();
        assertNotNull(info);

        // 3) Revoke the key
        userApi.apiKeysRevoke(keyPk);

        // 4) The same call now fails — a revoked key is not accepted anymore.
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, adminByKey::adminInfo);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getStatusCode().value());
    }

    private ApiClient newApiKeyClient(String token) {
        ApiClient client = new ApiClient();
        client.setBasePath(RestAssured.DEFAULT_URI + ":" + port + "/api/v1");
        client.addDefaultHeader(ApiKeyProperties.DEFAULT_HEADER_NAME, token);
        return client;
    }
}
