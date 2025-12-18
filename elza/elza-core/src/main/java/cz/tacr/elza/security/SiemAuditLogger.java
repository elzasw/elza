package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class SiemAuditLogger {
	private static final Logger LOG = LoggerFactory.getLogger("siem.audit");
	
	public enum EventType {
		AUTHENTICATION
	}
	
	public enum Outcome {
		SUCCESS,
		FAILURE
	}
		
	public void loginSuccess(String user, String sourceIp) {
		LOG.info("login_success", 
				kv("eventType", EventType.AUTHENTICATION.toString()), 
				kv("outcome", Outcome.SUCCESS.toString()), 
				kv("user", user),
				kv("sourceIp", sourceIp)
				);
		}

	public void loginFailed(String username, String sourceIp, String detail) {
		LOG.info("login_success", 
				kv("eventType", EventType.AUTHENTICATION.toString()), 
				kv("outcome", Outcome.FAILURE.toString()), 
				kv("user", username),
				kv("sourceIp", sourceIp),
				kv("detail", detail)
				);		
	}
}
