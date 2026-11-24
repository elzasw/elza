package cz.tacr.elza.security.oauth2;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrPermission.PermissionType;
import jakarta.annotation.PostConstruct;

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

    /**
     * Mapping of the token's authorities to permissions.
     *
     * {@code null} (the key is not configured) means that permissions are managed in ELZA only and
     * a login with a token does not touch them. Once the key is configured, even as an empty list,
     * the user's directly assigned permissions are replaced by the mapped ones at every login.
     */
    private List<PermProperties> permissions;

    /**
     * Create a user who does not exist in ELZA yet. When disabled, only users created in ELZA can
     * log in with a token.
     */
    private boolean createUsers = true;

    /**
     * Claim holding the user name.
     */
    private String usernameClaim = "sub";

    /**
     * Claim holding the display name; used only when a user is created.
     */
    private String nameClaim = "name";

    /**
     * Claim holding the list of authorities mapped by {@link #permissions}.
     */
    private String authoritiesClaim = "authorities";

    /**
     * Expected value of the {@code iss} claim; not checked when empty.
     */
    private String issuer;

    /**
     * Accepted values of the {@code aud} claim; the token must contain at least one of them. Not
     * checked when empty.
     */
    private List<String> audiences;

    /**
     * Scope of the archival entities of users created from tokens.
     */
    private String userScope = "JWT_USERS";

    /**
     * Class of the archival entities of users created from tokens; it must be assignable in the rule
     * set of {@link #userScope}.
     */
    private String userApType = "PERSON_INDIVIDUAL";

    /**
     * Checks the permission mapping at startup. A mapping that cannot be applied would otherwise
     * fail every login of a user with the authority.
     */
    @PostConstruct
    public void validate() {
        if (permissions == null) {
            return;
        }
        for (PermProperties permProps : permissions) {
            if (StringUtils.isBlank(permProps.getAuthority())) {
                throw new IllegalStateException("elza.security.o-auth2.permissions: authority is not set");
            }
            if (permProps.getPermissions() == null) {
                continue;
            }
            for (String permName : permProps.getPermissions()) {
                validatePermission(permProps, permName);
            }
        }
    }

    private void validatePermission(PermProperties permProps, String permName) {
        String context = "elza.security.o-auth2.permissions, authority " + permProps.getAuthority()
                + ", permission " + permName;
        Permission permission;
        try {
            permission = Permission.valueOf(permName);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(context + ": unknown permission", e);
        }
        // The mapping can name only a scope, so permissions bound to a fund, a user, a group, ...
        // cannot be granted from a token.
        PermissionType type = permission.getType();
        if (type == PermissionType.SCOPE) {
            if (StringUtils.isBlank(permProps.getScope())) {
                throw new IllegalStateException(context + ": the permission requires a scope");
            }
        } else if (type != PermissionType.ALL) {
            throw new IllegalStateException(context + ": permissions of type " + type
                    + " cannot be assigned from a token");
        }
    }

    /**
     * @return true when the user's directly assigned permissions are synchronized from the token
     */
    public boolean isPermissionsManaged() {
        return permissions != null;
    }

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

    public boolean isCreateUsers() {
        return createUsers;
    }

    public void setCreateUsers(boolean createUsers) {
        this.createUsers = createUsers;
    }

    public String getUsernameClaim() {
        return usernameClaim;
    }

    public void setUsernameClaim(String usernameClaim) {
        this.usernameClaim = usernameClaim;
    }

    public String getNameClaim() {
        return nameClaim;
    }

    public void setNameClaim(String nameClaim) {
        this.nameClaim = nameClaim;
    }

    public String getAuthoritiesClaim() {
        return authoritiesClaim;
    }

    public void setAuthoritiesClaim(String authoritiesClaim) {
        this.authoritiesClaim = authoritiesClaim;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }
}
