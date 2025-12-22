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

	private boolean kerberosClientDebug = false;

	private boolean ticketValidatorDebug = false;
	
	/**
	 * Flag to allow authentication with password over Kerberos
	 */
	private boolean authenticateWithPassword = true;

	private String ldapSearchBase;

	private String ldapSearchFilter;

	
	
	public boolean isAuthenticateWithPassword() {
		return authenticateWithPassword;
	}

	public void setAuthenticateWithPassword(boolean authenticateWithPassword) {
		this.authenticateWithPassword = authenticateWithPassword;
	}

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
