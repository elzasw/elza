package cz.tacr.elza.connector;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import cz.tacr.da.controller.DefaultApi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaInstance {

    private static final Logger logger = LoggerFactory.getLogger(DaInstance.class);

    private final DefaultApi defaultApi;

    private final Client fileTransferClient;

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
        fileTransferClient = FileTransfer.createClient(createClientConfig(url, username, password));
        fileTransferClient.start();
        logger.debug("Inicializován konektor na DA: {} (apiKey: {})", apiUrl, apiKey);
    }

    private ClientConfig createClientConfig(final String url, final String username, final String password) {
        ClientConfig clientConfig = new ClientConfig(url);
    	if (username != null) {
            ClientConfig.Authorization authorization = new ClientConfig.Authorization();
            authorization.setAuthorizationType("Basic");
            authorization.setPassword(password);
            authorization.setUsername(username);
            clientConfig.setAuthorization(authorization);
    	}
        return clientConfig;
    }

    public String getApiUrl() {
        return url;
    }

    public DefaultApi getDefaultApi() {
        return defaultApi;
    }

    public Client getFileTransferClient() {
        return fileTransferClient;
    }

    public void stopFileTransferClient() {
        fileTransferClient.stop();
    }
}
