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
