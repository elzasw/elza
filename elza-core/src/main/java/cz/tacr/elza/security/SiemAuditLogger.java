package cz.tacr.elza.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class SiemAuditLogger {
	private static final Logger LOG = LoggerFactory.getLogger("siem.audit");
	
	public void loginSuccess(String user, String ip) {
		LOG.info("login_success", 
				kv("eventType", "AUTHENTICATION"), 
				kv("outcome", "SUCCESS"), 
				kv("user", user),
				kv("sourceIp", ip)
				);
		}
}
