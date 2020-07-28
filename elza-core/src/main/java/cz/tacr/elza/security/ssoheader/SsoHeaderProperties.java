package cz.tacr.elza.security.ssoheader;

import javax.validation.constraints.NotEmpty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConditionalOnProperty(prefix = "elza.security.sso-header", name= "user-header")
@ConfigurationProperties(prefix = "elza.security.sso-header", ignoreUnknownFields = false)
public class SsoHeaderProperties {

	@NotEmpty
	private String userHeader;

	public String getUserHeader() {
		return userHeader;
	}

	public void setUserHeader(String userHeader) {
		this.userHeader = userHeader;
	}
}
