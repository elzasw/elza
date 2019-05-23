package cz.tacr.elza.dao.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.yaml.snakeyaml.Yaml;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoDigitizationService;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;

@SuppressWarnings("unchecked")
public class CoreServiceProvider {

	private static final Map<String, String> EXTERNAL_SYSTEMS_CONFIG;
	static {
		Path path = PathResolver.resolveExternalSystemsConfigPath();
		try (InputStream os = Files.newInputStream(path)) { 
			Object config = new Yaml().load(os);
			EXTERNAL_SYSTEMS_CONFIG = config != null ? (Map<String, String>) config : new HashMap<>();
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
		String addr = EXTERNAL_SYSTEMS_CONFIG.get(systemIdentifier);
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
			T t = factory.create(serviceClass);
			setBasicAuthorization(t);
			return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	private static <T> void setBasicAuthorization(final T t) {
		Client client = ClientProxy.getClient(t);
		HTTPConduit http = (HTTPConduit) client.getConduit();
		AuthorizationPolicy authPolicy = new AuthorizationPolicy();
		authPolicy.setAuthorizationType("Basic");
		authPolicy.setUserName(EXTERNAL_SYSTEMS_CONFIG.get("user"));
		authPolicy.setPassword(EXTERNAL_SYSTEMS_CONFIG.get("pass"));
		http.setAuthorization(authPolicy);
	}
}
