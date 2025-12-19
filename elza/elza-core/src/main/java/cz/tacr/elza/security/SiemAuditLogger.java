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
		// MS Active Directory
		ACTIVE_DIRECTORY
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
}
