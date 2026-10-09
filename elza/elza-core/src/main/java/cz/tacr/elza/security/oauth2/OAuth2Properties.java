package cz.tacr.elza.security.oauth2;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConditionalOnProperty(prefix = "elza.security.o-auth2", name = "key-url")
@ConfigurationProperties(prefix = "elza.security.o-auth2", ignoreUnknownFields = false)
public class OAuth2Properties {

    /**
     * Properties for one authority
     */
    static public class PermProperties {
        private String authority;
        private String scope;

        private List<String> permissions;

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }
    };

    private String keyUrl;

    private List<PermProperties> permissions;

    /**
     * Scope of the archival entities of users created from tokens.
     */
    private String userScope = "JWT_USERS";

    /**
     * Class of the archival entities of users created from tokens; it must be assignable in the rule
     * set of {@link #userScope}.
     */
    private String userApType = "PERSON_INDIVIDUAL";

    public String getUserScope() {
        return userScope;
    }

    public void setUserScope(String userScope) {
        this.userScope = userScope;
    }

    public String getUserApType() {
        return userApType;
    }

    public void setUserApType(String userApType) {
        this.userApType = userApType;
    }

    public String getKeyUrl() {
        return keyUrl;
    }

    public void setKeyUrl(String keyUrl) {
        this.keyUrl = keyUrl;
    }

    public List<PermProperties> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermProperties> permissions) {
        this.permissions = permissions;
    }
}
