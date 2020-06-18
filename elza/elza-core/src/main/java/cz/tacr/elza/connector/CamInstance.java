package cz.tacr.elza.connector;

import cz.tacr.cam.client.controller.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamInstance {

    private static final Logger logger = LoggerFactory.getLogger(CamInstance.class);

    private SearchApi searchApi;

    private EntityApi entityApi;

    private ExportApi exportApi;

    private BatchUpdatesApi batchUpdatesApi;

    private UpdatesApi updatesApi;

    public void init(String url, String apiKey, String apiValue) {
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("Není nastavena properta cam.url pro připojení api");
        } else if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Není nastavena properta cam.api-key pro připojení api");
        } else if (StringUtils.isEmpty(apiValue)) {
            throw new IllegalArgumentException("Není nastavena properta cam.api-value pro připojení api");
        }
        ApiClientCam apiClientCam = new ApiClientCam(url, apiKey, apiValue);
        searchApi = new SearchApi(apiClientCam);
        entityApi = new EntityApi(apiClientCam);
        exportApi = new ExportApi(apiClientCam);
        batchUpdatesApi = new BatchUpdatesApi(apiClientCam);
        updatesApi = new UpdatesApi(apiClientCam);
        logger.info("Inicializován konektor na CAM: {} (apiKey: {})", url, apiKey);
    }

    public SearchApi getSearchApi() {
        return searchApi;
    }

    public void setSearchApi(SearchApi searchApi) {
        this.searchApi = searchApi;
    }

    public EntityApi getEntityApi() {
        return entityApi;
    }

    public void setEntityApi(EntityApi entityApi) {
        this.entityApi = entityApi;
    }

    public ExportApi getExportApi() {
        return exportApi;
    }

    public void setExportApi(ExportApi exportApi) {
        this.exportApi = exportApi;
    }

    public BatchUpdatesApi getBatchUpdatesApi() {
        return batchUpdatesApi;
    }

    public void setBatchUpdatesApi(BatchUpdatesApi batchUpdatesApi) {
        this.batchUpdatesApi = batchUpdatesApi;
    }

    public UpdatesApi getUpdatesApi() {
        return updatesApi;
    }

    public void setUpdatesApi(UpdatesApi updatesApi) {
        this.updatesApi = updatesApi;
    }
}
