package cz.tacr.elza.security.apikey;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration of the personal API key feature.
 *
 * Unlike SSO/OAuth, this feature is always available with defaults — turning it off
 * (setting {@code enabled=false}) makes the server ignore the header on every request.
 */
@Component
@ConfigurationProperties(prefix = "elza.security.api-keys", ignoreUnknownFields = false)
public class ApiKeyProperties {

    /** Default value of {@link #headerName}. */
    public static final String DEFAULT_HEADER_NAME = "X-API-Key";

    /** When {@code false}, the header is ignored and the request falls through to the standard chain. */
    private boolean enabled = true;

    /** HTTP header the token is read from. */
    private String headerName = DEFAULT_HEADER_NAME;

    /** Validity applied to a new key when the user does not choose one. */
    private int defaultValidityDays = 365;

    /** Upper bound the UI and API accept for the validity of a new key. */
    private int maxValidityDays = 730;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public int getDefaultValidityDays() {
        return defaultValidityDays;
    }

    public void setDefaultValidityDays(int defaultValidityDays) {
        this.defaultValidityDays = defaultValidityDays;
    }

    public int getMaxValidityDays() {
        return maxValidityDays;
    }

    public void setMaxValidityDays(int maxValidityDays) {
        this.maxValidityDays = maxValidityDays;
    }
}
