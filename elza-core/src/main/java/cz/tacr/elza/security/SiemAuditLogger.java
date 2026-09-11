package cz.tacr.elza.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class SiemAuditLogger {
	private static final Logger LOG = LoggerFactory.getLogger("siem.audit");
	
	// Get the property, defaulting to an empty string if not found
    @Value("${elza.siemLogFile:}")
    private String siemLogPath;
    
    public enum EventType {
		AUTHENTICATION
	}
    
    public enum AuthenticationType {
		PASSWORD,
		JWT,
		SSO_HEADER,
		// MS Active Directory - direct password authentication
		ACTIVE_DIRECTORY,
		// Kerberos based authentication - suitable for SSO
		KERBEROS,
		// Personal API key sent in the X-API-Key header
		API_KEY
	}
	
	public enum Outcome {
		SUCCESS,
		FAILURE
	}

	@PostConstruct
    public void init() {
        if (StringUtils.isEmpty(siemLogPath)) {
            // Programmatically disable the "siem.audit" logger
        	var ctx = LoggerFactory.getILoggerFactory();
        	if(ctx instanceof LoggerContext) {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                ch.qos.logback.classic.Logger logbackLogger = context.getLogger("siem.audit");
                logbackLogger.setLevel(Level.OFF);
	        	
        	}
        }
    }
	
	public void loginSuccess(String user, String sourceIp, AuthenticationType authenticationType) {
		var builder = LOG.atInfo().setMessage("login_success");
		builder.addArgument(kv("eventType", EventType.AUTHENTICATION.toString()))
				.addArgument(kv("outcome", Outcome.SUCCESS.toString()))
				.addArgument(kv("eventType", EventType.AUTHENTICATION.toString()))
				.addArgument(kv("user", user))
				;
		if(authenticationType!=null) {
			builder.addArgument(kv("authenticationType", authenticationType.toString()));
		}
		if(sourceIp!=null) {
			builder.addArgument(kv("sourceIp", sourceIp));
		}
		builder.log();
	}

	public void loginFailed(String username, String sourceIp, String detail) {
		var builder = LOG.atInfo().setMessage("login_failed");
		builder.addArgument(kv("eventType", EventType.AUTHENTICATION.toString()))
				.addArgument(kv("outcome", Outcome.FAILURE.toString()))
				.addArgument(kv("user", username))
				;
		if(sourceIp!=null) {
			builder.addArgument(kv("sourceIp", sourceIp));
		}
		if(detail!=null) {
			builder.addArgument(kv("detail", detail));
		}
		builder.log();
	}

	/** Successful API-key authentication — includes the public key id. */
	public void apiKeyLoginSuccess(String user, String sourceIp, String keyId) {
		var builder = LOG.atInfo().setMessage("login_success")
				.addArgument(kv("eventType", EventType.AUTHENTICATION.toString()))
				.addArgument(kv("outcome", Outcome.SUCCESS.toString()))
				.addArgument(kv("authenticationType", AuthenticationType.API_KEY.toString()))
				.addArgument(kv("user", user))
				.addArgument(kv("keyId", keyId));
		if (sourceIp != null) {
			builder.addArgument(kv("sourceIp", sourceIp));
		}
		builder.log();
	}

	/** Failed API-key authentication — keyId is null for MALFORMED_TOKEN. */
	public void apiKeyLoginFailed(String sourceIp, String keyId, String detail) {
		var builder = LOG.atInfo().setMessage("login_failed")
				.addArgument(kv("eventType", EventType.AUTHENTICATION.toString()))
				.addArgument(kv("outcome", Outcome.FAILURE.toString()))
				.addArgument(kv("authenticationType", AuthenticationType.API_KEY.toString()));
		if (keyId != null) {
			builder.addArgument(kv("keyId", keyId));
		}
		if (sourceIp != null) {
			builder.addArgument(kv("sourceIp", sourceIp));
		}
		if (detail != null) {
			builder.addArgument(kv("detail", detail));
		}
		builder.log();
	}

	/** API key was created. Actor is the person who logged in; owner is the key's user. */
	public void apiKeyCreated(String actor, String owner, String keyId, java.time.OffsetDateTime expireDate) {
		LOG.atInfo().setMessage("api_key_created")
				.addArgument(kv("actor", actor))
				.addArgument(kv("owner", owner))
				.addArgument(kv("keyId", keyId))
				.addArgument(kv("expireDate", expireDate.toString()))
				.log();
	}

	/** API key was revoked. Actor is the person who revoked it (owner or an admin). */
	public void apiKeyRevoked(String actor, String owner, String keyId) {
		LOG.atInfo().setMessage("api_key_revoked")
				.addArgument(kv("actor", actor))
				.addArgument(kv("owner", owner))
				.addArgument(kv("keyId", keyId))
				.log();
	}
}
