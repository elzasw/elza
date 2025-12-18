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
		
	public void loginSuccess(String user, String ip) {
		LOG.info("login_success", 
				kv("eventType", EventType.AUTHENTICATION), 
				kv("outcome", Outcome.SUCCESS), 
				kv("user", user),
				kv("sourceIp", ip)
				);
		}

	public void loginFailed(String username, String sourceIp, String detail) {
		LOG.info("login_success", 
				kv("eventType", EventType.AUTHENTICATION), 
				kv("outcome", Outcome.FAILURE), 
				kv("user", username),
				kv("sourceIp", sourceIp),
				kv("detail", detail)
				);		
	}
}
