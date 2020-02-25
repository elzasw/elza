package cz.tacr.elza.dao.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoDigitizationService;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;

@SuppressWarnings("unchecked")
public class CoreServiceProvider {

    static Logger log = LoggerFactory.getLogger(CoreServiceProvider.class);

	private static final Map<String, Object> EXTERNAL_SYSTEMS_CONFIG;
	static {
		Path path = PathResolver.resolveExternalSystemsConfigPath();
        log.debug("Loading file: " + path);

		try (InputStream os = Files.newInputStream(path)) { 
			Object config = new Yaml().load(os);
			if(config != null) { 
			    EXTERNAL_SYSTEMS_CONFIG = (Map<String, Object>) config;
                log.debug("Loaded external configurations, entries: " + EXTERNAL_SYSTEMS_CONFIG.size());
                for (Entry<String, Object> cfg : EXTERNAL_SYSTEMS_CONFIG.entrySet()) {
                    log.debug(" " + cfg.getKey());
			    }
			} else {
			    EXTERNAL_SYSTEMS_CONFIG = new HashMap<>();
			    log.debug("Empty external system configuration");
			}
		} catch (Exception e) {
			throw new DaoComponentException("failed to load external systems config", e);
		}
	}

	public static DaoRequestsService getDaoRequestsService(String systemIdentifier) {
		return getWsClient(DaoRequestsService.class, CoreService.DaoRequestsService, systemIdentifier);
	}

	public static DaoDigitizationService getDaoDigitizationService(String systemIdentifier) {
		return getWsClient(DaoDigitizationService.class, CoreService.DaoDigitizationService, systemIdentifier);
	}

	public static DaoService getDaoCoreService(String systemIdentifier) {
		return getWsClient(DaoService.class, CoreService.DaoCoreService, systemIdentifier);
	}

	private static <T> T getWsClient(Class<T> serviceClass, QName serviceName, String systemIdentifier) {
        log.debug("GetWsClient: serviceName: {}, systemIdentifier: {}", serviceName, systemIdentifier);

        Object systemInfo = EXTERNAL_SYSTEMS_CONFIG.get(systemIdentifier);

		String addr = null;
		String user = null;
		String password = null;
		if(systemInfo instanceof String) {
			addr = (String) systemInfo;
		} else {
			Map<String, String> systemInfoMap = (Map<String, String>) systemInfo;
			addr = systemInfoMap.get("address");
			user = systemInfoMap.get("user");
			password = systemInfoMap.get("password");
		}
		if (addr == null || (addr = addr.trim()).length() == 0) {
			throw new DaoComponentException("external system address not found, systemIdentifier:" + systemIdentifier);
		}
		if (addr.charAt(addr.length() - 1) != '/') {
			addr += '/';
		}
		addr += serviceName.getLocalPart();
		try {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceName(serviceName);
            factory.setAddress(addr);
            if(user != null) {
                log.debug("Setting basic authorization for: {} / {}", user, password);

            	factory.setUsername(user);
            	factory.setPassword(password);
                //setBasicAuthorization(t);
            }
            T t = factory.create(serviceClass);
			return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*protected static <T> void setBasicAuthorization(final T t, String user, String password) {
        Client client = ClientProxy.getClient(t);
        if (StringUtils.isNotBlank(user)) {
            log.debug("Setting basic authorization for: {} / {}", user, password);
        
            HTTPConduit http = (HTTPConduit) client.getConduit();
            AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setAuthorizationType("Basic");
            authPolicy.setUserName(user);
            authPolicy.setPassword(password);
            http.setAuthorization(authPolicy);
        }
    }*/
}
