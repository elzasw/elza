package cz.tacr.elza.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Users whose password from the configuration (elza.security.override) is accepted next to
 * their own password in the database. A missing user is created as an administrator at startup.
 */
@Component
@ConfigurationProperties(prefix = "elza.security")
public class OverrideProperties {

    private List<OverrideUser> override = new ArrayList<>();

    public static class OverrideUser {

        private String username;

        /** Plain text. */
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }
    }

    public List<OverrideUser> getOverride() {
        return override;
    }

    public void setOverride(final List<OverrideUser> override) {
        this.override = override;
    }

    /** Fails on an entry without a user name or a password, or a user listed twice. */
    public void validate() {
        Set<String> names = new HashSet<>();
        for (OverrideUser user : override) {
            if (StringUtils.isBlank(user.getUsername()) || StringUtils.isEmpty(user.getPassword())) {
                throw new IllegalStateException("Položka elza.security.override musí mít username i password.");
            }
            if (!names.add(user.getUsername().trim())) {
                throw new IllegalStateException("Uživatel " + user.getUsername().trim()
                        + " je v elza.security.override uveden vícekrát.");
            }
        }
    }

    /** Whether the configuration lists the user. */
    public boolean contains(final String username) {
        return find(username) != null;
    }

    /** Whether the password is the configured one of the user; constant-time comparison. */
    public boolean matches(final String username, final String password) {
        OverrideUser user = find(username);
        return user != null && user.getPassword() != null && password != null
                && MessageDigest.isEqual(user.getPassword().getBytes(StandardCharsets.UTF_8),
                                         password.getBytes(StandardCharsets.UTF_8));
    }

    private OverrideUser find(final String username) {
        if (username == null) {
            return null;
        }
        return override.stream()
                .filter(u -> u.getUsername() != null && u.getUsername().trim().equals(username))
                .findFirst()
                .orElse(null);
    }
}
