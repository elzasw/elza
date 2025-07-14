package cz.tacr.elza.security.kerberos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
@Component
@ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
@ConfigurationProperties(prefix = "elza.security.kerberos", ignoreUnknownFields = false)
public class KerberosProperties {

	/**
	 * Name of the service principal
	 */
	@NotEmpty
	private String servicePrincipal;

	/**
	 * Name of the keytab with secret for the service principal
	 */
	@NotEmpty
	private String keytabLocation;

	private String adDomain;

	private String adServer;

	private boolean kerberosClientDebug = false;

	private boolean ticketValidatorDebug = false;

	private String ldapSearchBase;

	private String ldapSearchFilter;

	public String getServicePrincipal() {
		return servicePrincipal;
	}

	public void setServicePrincipal(String servicePrincipal) {
		this.servicePrincipal = servicePrincipal;
	}

	public String getKeytabLocation() {
		return keytabLocation;
	}

	public void setKeytabLocation(String keytabLocation) {
		this.keytabLocation = keytabLocation;
	}

	public boolean isKerberosClientDebug() {
		return kerberosClientDebug;
	}

	public void setKerberosClientDebug(boolean kerberosClientDebug) {
		this.kerberosClientDebug = kerberosClientDebug;
	}

	public boolean isTicketValidatorDebug() {
		return ticketValidatorDebug;
	}

	public void setTicketValidatorDebug(boolean ticketValidatorDebug) {
		this.ticketValidatorDebug = ticketValidatorDebug;
	}

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

	public String getLdapSearchBase() {
		return ldapSearchBase;
	}

	public void setLdapSearchBase(String ldapSearchBase) {
		this.ldapSearchBase = ldapSearchBase;
	}

	public String getLdapSearchFilter() {
		return ldapSearchFilter;
	}
	
	public void setLdapSearchFilter(String ldapSearchFilter) {
		this.ldapSearchFilter = ldapSearchFilter;
	}
}
