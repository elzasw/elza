package cz.tacr.elza.security.ldap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConditionalOnProperty(prefix = "elza.security.ldap", name = "ad-domain")
@ConfigurationProperties(prefix = "elza.security.ldap", ignoreUnknownFields = false)
public class LdapProperties {
	private String adDomain;

	private String adServer;

	public String getAdDomain() {
		return adDomain;
	}

	public void setAdDomain(String adDomain) {
		this.adDomain = adDomain;
	}

	public String getAdServer() {
		return adServer;
	}

	public void setAdServer(String adServer) {
		this.adServer = adServer;
	}

}
