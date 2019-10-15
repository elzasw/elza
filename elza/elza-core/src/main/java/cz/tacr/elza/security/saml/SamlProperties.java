package cz.tacr.elza.security.saml;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("elza.saml")
@ConditionalOnProperty(prefix = "elza.saml", name = "entity-id")
public class SamlProperties {

    @NotEmpty
    private String entityId;

    @NotEmpty
    private String entityBaseUrl;

    @NotEmpty
    private String title = "Saml";

    @NotNull
    private ContextProvider contextProvider;

    @NotNull
    private KeyManager keyManager;

    @NotEmpty
    private String idpUrl;

    @NotEmpty
    private String idpMetaUrl;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    public String getEntityBaseUrl() {
        return entityBaseUrl;
    }

    public void setEntityBaseUrl(final String entityBaseUrl) {
        this.entityBaseUrl = entityBaseUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getIdpUrl() {
        return idpUrl;
    }

    public void setIdpUrl(final String idpUrl) {
        this.idpUrl = idpUrl;
    }

    public String getIdpMetaUrl() {
        return idpMetaUrl;
    }

    public void setIdpMetaUrl(final String idpMetaUrl) {
        this.idpMetaUrl = idpMetaUrl;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public void setKeyManager(final KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public ContextProvider getContextProvider() {
        return contextProvider;
    }

    public void setContextProvider(final ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    public static class KeyManager {

        @NotEmpty
        private String path;

        @NotEmpty
        private String key;

        @NotEmpty
        private String password;

        public String getPath() {
            return path;
        }

        public void setPath(final String path) {
            this.path = path;
        }

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }
    }

    public static class ContextProvider {

        @NotEmpty
        private String serverName;

        @NotEmpty
        private String scheme;

        @Min(1)
        @Max(65536)
        private int serverPort;

        private boolean includeServerPortInRequestUrl;


        private String contextPath;

        public String getServerName() {
            return serverName;
        }

        public void setServerName(final String serverName) {
            this.serverName = serverName;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(final String scheme) {
            this.scheme = scheme;
        }

        public int getServerPort() {
            return serverPort;
        }

        public void setServerPort(final int serverPort) {
            this.serverPort = serverPort;
        }

        public boolean isIncludeServerPortInRequestUrl() {
            return includeServerPortInRequestUrl;
        }

        public void setIncludeServerPortInRequestUrl(final boolean includeServerPortInRequestUrl) {
            this.includeServerPortInRequestUrl = includeServerPortInRequestUrl;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(final String contextPath) {
            this.contextPath = contextPath;
        }
    }
}
