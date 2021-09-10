package cz.tacr.elza.connector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.cam.client.controller.BatchUpdatesApi;
import cz.tacr.cam.client.controller.EntityApi;
import cz.tacr.cam.client.controller.ExportApi;
import cz.tacr.cam.client.controller.SearchApi;
import cz.tacr.cam.client.controller.UpdatesApi;

public class CamInstance {

    public static final String API_URL = "/api/v1";
    public static final String DETAIL_URL = "/global/";

    private static final Logger logger = LoggerFactory.getLogger(CamInstance.class);

    private final SearchApi searchApi;
    private final EntityApi entityApi;
    private final ExportApi exportApi;
    private final BatchUpdatesApi batchUpdatesApi;
    private final UpdatesApi updatesApi;

    private final String url;

    public CamInstance(final String url, final String apiKey, final String apiValue) {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Není nastavena properta cam.url pro připojení api");
        } else if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Není nastavena properta cam.api-key pro připojení api");
        } else if (StringUtils.isEmpty(apiValue)) {
            throw new IllegalArgumentException("Není nastavena properta cam.api-value pro připojení api");
        }
        this.url = StringUtils.stripEnd(url.trim(), "/");
        String apiUrl = getApiUrl();
        ApiClientCam apiClientCam = new ApiClientCam(apiUrl, apiKey, apiValue);
        searchApi = new SearchApi(apiClientCam);
        entityApi = new EntityApi(apiClientCam);
        exportApi = new ExportApi(apiClientCam);
        batchUpdatesApi = new BatchUpdatesApi(apiClientCam);
        updatesApi = new UpdatesApi(apiClientCam);
        logger.debug("Inicializován konektor na CAM: {} (apiKey: {})", apiUrl, apiKey);
    }

    public String getApiUrl() {
        return url + API_URL;
    }

    public String getEntityDetailUrl(String entityCode) {
        return url + DETAIL_URL + entityCode;
    }

    public SearchApi getSearchApi() {
        return searchApi;
    }

    public EntityApi getEntityApi() {
        return entityApi;
    }

    public ExportApi getExportApi() {
        return exportApi;
    }

    public BatchUpdatesApi getBatchUpdatesApi() {
        return batchUpdatesApi;
    }

    public UpdatesApi getUpdatesApi() {
        return updatesApi;
    }
}
