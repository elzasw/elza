package cz.tacr.elza.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.security.OverrideProperties.OverrideUser;

/**
 * {@link OverrideProperties}: lookup and validation of the users from elza.security.override.
 */
class OverridePropertiesTest {

    private static OverrideProperties properties(final String... usernameAndPassword) {
        List<OverrideUser> users = new ArrayList<>();
        for (int i = 0; i < usernameAndPassword.length; i += 2) {
            OverrideUser user = new OverrideUser();
            user.setUsername(usernameAndPassword[i]);
            user.setPassword(usernameAndPassword[i + 1]);
            users.add(user);
        }
        OverrideProperties properties = new OverrideProperties();
        properties.setOverride(users);
        return properties;
    }

    @Test
    void matchesOnlyTheConfiguredPasswordOfTheUser() {
        OverrideProperties properties = properties(" spravce ", "heslo", "druhy", "jine");

        assertThat(properties.contains("spravce")).isTrue();
        assertThat(properties.matches("spravce", "heslo")).isTrue();
        assertThat(properties.matches("spravce", "jine")).isFalse();
        assertThat(properties.matches("spravce", null)).isFalse();
        assertThat(properties.contains("nikdo")).isFalse();
        assertThat(properties.matches("nikdo", "heslo")).isFalse();
        assertThat(properties.matches(null, "heslo")).isFalse();
    }

    @Test
    void emptyConfigurationIsValid() {
        OverrideProperties properties = new OverrideProperties();

        properties.validate();
        assertThat(properties.contains("spravce")).isFalse();
    }

    @Test
    void incompleteEntryIsInvalid() {
        assertThatThrownBy(() -> properties("spravce", null).validate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> properties(" ", "heslo").validate()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void duplicateUserIsInvalid() {
        assertThatThrownBy(() -> properties("spravce", "a", "spravce ", "b").validate())
                .isInstanceOf(IllegalStateException.class);
    }
}
