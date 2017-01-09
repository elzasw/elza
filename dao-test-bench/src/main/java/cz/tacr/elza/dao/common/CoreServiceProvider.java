package cz.tacr.elza.dao.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.yaml.snakeyaml.Yaml;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoDigitizationService;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;

public class CoreServiceProvider {

	private static final CoreService CORE_SERVICE = new CoreService();

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
		DaoRequestsService service = CORE_SERVICE.getDaoRequestsService();
		setEndPointAddress((BindingProvider) service, systemIdentifier);
		return service;
	}

	public static DaoDigitizationService getDaoDigitizationService(String systemIdentifier) {
		DaoDigitizationService service = CORE_SERVICE.getDaoDigitizationService();
		setEndPointAddress((BindingProvider) service, systemIdentifier);
		return service;
	}

	public static DaoService getDaoService(String systemIdentifier) {
		DaoService service = CORE_SERVICE.getDaoCoreService();
		setEndPointAddress((BindingProvider) service, systemIdentifier);
		return service;
	}

	private static void setEndPointAddress(BindingProvider bindingProvider, String systemIdentifier) {
		String systemAddress = EXTERNAL_SYSTEMS_CONFIG.get(systemIdentifier);
		if (systemAddress == null) {
			throw new DaoComponentException(
					"external system address not found, systemIdentifier:" + systemIdentifier);
		}
		Map<String, Object> context = bindingProvider.getRequestContext();
		context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, systemAddress);
	}
}