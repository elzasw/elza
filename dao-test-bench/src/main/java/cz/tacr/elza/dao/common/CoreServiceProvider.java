package cz.tacr.elza.dao.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.yaml.snakeyaml.Yaml;

import cz.tacr.elza.dao.exception.DaoComponentException;
import cz.tacr.elza.ws.core.v1.CoreService;
import cz.tacr.elza.ws.core.v1.DaoDigitizationService;
import cz.tacr.elza.ws.core.v1.DaoRequestsService;
import cz.tacr.elza.ws.core.v1.DaoService;

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
		addr += addr.endsWith("/") ? serviceName.getLocalPart() : '/' + serviceName.getLocalPart();
		try {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceName(serviceName);
            factory.setAddress(addr);
            factory.getOutInterceptors().add(new SoapActionInterceptor());
            return factory.create(serviceClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	public static class SoapActionInterceptor extends AbstractSoapInterceptor {
	    private static final String SLASH = "/";

	    public SoapActionInterceptor() {
	        super(Phase.SEND);
	    }

	    @Override
	    public void handleMessage(SoapMessage message) throws Fault {

	        BindingOperationInfo bindingOperationInfo = message.getExchange().getBindingOperationInfo();
	        OperationInfo operationInfo = bindingOperationInfo.getOperationInfo();
	        InterfaceInfo interfaceInfo = operationInfo.getInterface();
	        QName interfaceInfoNameQName = interfaceInfo.getName();
	        QName operationQName = operationInfo.getName();

	        Map<String, List<String>> reqHeaders = CastUtils.cast((Map<?, ?>) message.get(Message.PROTOCOL_HEADERS));

	        if (reqHeaders == null) {
	            reqHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	        }

	        if (reqHeaders.size() == 0) {
	            message.put(Message.PROTOCOL_HEADERS, reqHeaders);
	        }

	        reqHeaders.put(SoapBindingConstants.SOAP_ACTION, Arrays.asList(interfaceInfoNameQName.getNamespaceURI() + SLASH + interfaceInfoNameQName.getLocalPart() + SLASH + operationQName.getLocalPart()));
	    }
	}
}