package cz.tacr.elza.connector;

import cz.tacr.da.controller.DefaultApi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaInstance {

    private static final Logger logger = LoggerFactory.getLogger(DaInstance.class);

    private final DefaultApi defaultApi;

    private final String url;

    public DaInstance(final String url, final String apiKey, final String apiValue) {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Není nastavena properta da.url pro připojení api");
        } else if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Není nastavena properta da.api-key pro připojení api");
        } else if (StringUtils.isEmpty(apiValue)) {
            throw new IllegalArgumentException("Není nastavena properta da.api-value pro připojení api");
        }
        this.url = StringUtils.stripEnd(url.trim(), "/");
        String apiUrl = getApiUrl();
        ApiClientDa apiClientDa = new ApiClientDa(apiUrl, apiKey, apiValue);
        defaultApi = new DefaultApi(apiClientDa);
        logger.debug("Inicializován konektor na DA: {} (apiKey: {})", apiUrl, apiKey);
    }

    public String getApiUrl() {
        return url;
    }

    public DefaultApi getDefaultApi() {
        return defaultApi;
    }
}
