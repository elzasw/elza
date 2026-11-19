package cz.tacr.elza.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;

import cz.tacr.elza.security.oauth2.OAuth2Properties.PermProperties;

class OAuth2PropertiesTest {

    private static OAuth2Properties bind(String yaml) throws IOException {
        var resource = new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8));
        var environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("test", resource)
                .forEach(environment.getPropertySources()::addFirst);
        return Binder.get(environment)
                .bindOrCreate("elza.security.o-auth2", OAuth2Properties.class);
    }

    private static OAuth2Properties withMapping(String scope, String... permissions) {
        PermProperties mapping = new PermProperties();
        mapping.setAuthority("ROLE");
        mapping.setScope(scope);
        mapping.setPermissions(List.of(permissions));
        OAuth2Properties props = new OAuth2Properties();
        props.setPermissions(List.of(mapping));
        return props;
    }

    @Test
    void defaultsKeepPreviousBehaviourExceptPermissions() throws IOException {
        OAuth2Properties props = bind("""
                elza:
                  security:
                    o-auth2:
                      key-url: https://idp.example/token_key
                """);

        assertTrue(props.isCreateUsers());
        assertFalse(props.isPermissionsManaged());
        assertEquals("sub", props.getUsernameClaim());
        assertEquals("name", props.getNameClaim());
        assertEquals("authorities", props.getAuthoritiesClaim());
    }

    @Test
    void emptyPermissionListManagesPermissions() throws IOException {
        OAuth2Properties props = bind("""
                elza:
                  security:
                    o-auth2:
                      key-url: https://idp.example/token_key
                      create-users: false
                      permissions: []
                """);

        assertFalse(props.isCreateUsers());
        assertTrue(props.isPermissionsManaged());
    }

    @Test
    void validMappingIsAccepted() {
        assertDoesNotThrow(withMapping(null, "ADMIN")::validate);
        assertDoesNotThrow(withMapping("GLOBAL", "AP_SCOPE_RD", "FUND_RD_ALL")::validate);
    }

    @Test
    void unknownPermissionFailsAtStartup() {
        assertThrows(IllegalStateException.class, withMapping(null, "NO_SUCH_PERMISSION")::validate);
    }

    @Test
    void scopePermissionWithoutScopeFailsAtStartup() {
        assertThrows(IllegalStateException.class, withMapping(null, "AP_SCOPE_RD")::validate);
    }

    @Test
    void fundPermissionFailsAtStartup() {
        assertThrows(IllegalStateException.class, withMapping(null, "FUND_RD")::validate);
    }
}
