package cz.tacr.elza.connector;

import com.lightcomp.ft.wsdl.v1.FileTransferService;
import com.lightcomp.ft.wsdl.v1.FileTransferService_Service;
import cz.tacr.da.controller.DefaultApi;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;

public class DaInstance {

    private static final Logger logger = LoggerFactory.getLogger(DaInstance.class);

    private final DefaultApi defaultApi;

    private final FileTransferService fileTransferService;

    private final String url;

    public DaInstance(final String url, final String apiKey, final String apiValue, final String username, final String password) {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Není nastavena properta da.url pro připojení api");
        } else if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Není nastavena properta da.api-key pro připojení api");
        } else if (StringUtils.isEmpty(apiValue)) {
            throw new IllegalArgumentException("Není nastavena properta da.api-value pro připojení api");
        } else if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Není nastavena properta da.username pro připojení api");
        } else if (StringUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Není nastavena properta da.password pro připojení api");
        }
        this.url = StringUtils.stripEnd(url.trim(), "/");
        String apiUrl = getApiUrl();
        ApiClientDa apiClientDa = new ApiClientDa(apiUrl, apiKey, apiValue);
        defaultApi = new DefaultApi(apiClientDa);
        fileTransferService = getWsClient(FileTransferService.class, FileTransferService_Service.SERVICE, url, username, password);
        logger.debug("Inicializován konektor na DA: {} (apiKey: {})", apiUrl, apiKey);
    }

    private static <T> T getWsClient(Class<T> serviceClass, QName serviceName, String url, String username, String password) {
        if (url.charAt(url.length() - 1) != '/') {
            url += '/';
        }
        url += serviceName.getLocalPart();
        try {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceName(serviceName);
            factory.setAddress(url);
            if (username != null) {
                factory.setUsername(username);
                factory.setPassword(password);
            }
            return factory.create(serviceClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getApiUrl() {
        return url;
    }

    public DefaultApi getDefaultApi() {
        return defaultApi;
    }

    public FileTransferService getFileTransferService() {
        return fileTransferService;
    }
}
