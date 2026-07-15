package cz.tacr.elza.security.ldap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Enables the Spring Boot {@code ldap} health indicator only when ELZA LDAP/AD
 * authentication is configured.
 *
 * Spring Boot auto-configures an LDAP client, and with it the {@code ldap} health
 * indicator, against {@code localhost:389} whenever spring-ldap is on the
 * classpath. ELZA authenticates through its own {@link ActiveDirectoryUserDetailProvider}
 * and never uses that client, so on an instance that does not use LDAP the
 * indicator drags the aggregate health down to DOWN for a server that is not
 * expected to exist. This processor keeps the indicator switched off unless
 * {@code elza.security.ldap.ad-domain} is set, and when it is, points the probe at
 * the configured domain controller ({@code elza.security.ldap.ad-server}) so the
 * check reflects the server ELZA actually authenticates against.
 *
 * The computed values are added at the lowest precedence, so an explicit
 * {@code management.health.ldap.enabled} or {@code spring.ldap.urls} in the
 * external configuration still wins.
 */
public class LdapHealthEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String adDomain = environment.getProperty("elza.security.ldap.ad-domain");
        boolean ldapConfigured = StringUtils.hasText(adDomain);

        Map<String, Object> props = new HashMap<>();
        props.put("management.health.ldap.enabled", ldapConfigured);
        if (ldapConfigured) {
            String adServer = environment.getProperty("elza.security.ldap.ad-server");
            if (StringUtils.hasText(adServer)) {
                props.put("spring.ldap.urls", adServer);
            }
        }
        environment.getPropertySources().addLast(new MapPropertySource("elzaLdapHealth", props));
    }

    @Override
    public int getOrder() {
        // Run after the configuration data (elza.yaml) has been loaded so the
        // elza.security.ldap.* properties are visible.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
