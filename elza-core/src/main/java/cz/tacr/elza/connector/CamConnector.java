package cz.tacr.elza.connector;

import cz.tacr.cam._2019.BatchUpdate;
import cz.tacr.cam._2019.BatchUpdateResult;
import cz.tacr.cam._2019.Entity;
import cz.tacr.cam._2019.QueryResult;
import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.client.ApiResponse;
import cz.tacr.cam.client.controller.*;
import cz.tacr.cam.client.controller.vo.QueryParamsDef;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.service.ExternalSystemService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class CamConnector {

    @Autowired
    private ExternalSystemService externalSystemService;

    private static final Logger logger = LoggerFactory.getLogger(CamConnector.class);

    private final Map<String, CamInstance> instanceMap = new HashMap<>();


    public QueryResult search(final int page,
                              final int pageSize,
                              final QueryParamsDef query,
                              final String externalSystemCode) throws ApiException {

        ApiResponse<File> fileApiResponse = getSearchApiByCode(externalSystemCode).searchApsWithHttpInfo(page, pageSize, query);
        return JaxbUtils.unmarshal(QueryResult.class, fileApiResponse.getData());
    }

    public Entity getEntityById(final Integer archiveEntityId,
                                final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getEntityApiByCode(externalSystemCode).getEntityByIdWithHttpInfo(String.valueOf(archiveEntityId));
        return JaxbUtils.unmarshal(Entity.class, fileApiResponse.getData());
    }

    public BatchUpdateResult postNewBatch(final BatchUpdate batchUpdate,
                                          final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getBatchUpdatesApiByCode(externalSystemCode).postNewBatchWithHttpInfo(JaxbUtils.asFile(batchUpdate));
        return JaxbUtils.unmarshal(BatchUpdateResult.class, fileApiResponse.getData());
    }

    public BatchUpdateResult getBatchStatus(final String bid,
                                            final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getBatchUpdatesApiByCode(externalSystemCode).getBatchStatusWithHttpInfo(bid);
        return JaxbUtils.unmarshal(BatchUpdateResult.class, fileApiResponse.getData());
    }

    public void invalidate(String code) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(code);
        if (apExternalSystem != null && apExternalSystem.getType() == ApExternalSystemType.CAM) {
            instanceMap.remove(code);
        }
    }

    public CamInstance getByCode(String code) {
        try {
            ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(code);
            if (apExternalSystem.getType() == ApExternalSystemType.CAM) {
                CamInstance camInstance = instanceMap.get(apExternalSystem.getCode());
                if (camInstance == null) {
                    camInstance = new CamInstance(apExternalSystem.getUrl(), apExternalSystem.getApiKeyId(), apExternalSystem.getApiKeyValue());
                    instanceMap.put(apExternalSystem.getCode(), camInstance);
                }
                return camInstance;
            } else {
                throw new IllegalArgumentException("Externí systém není typu CAM");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Externí systém nenalezen");
        }
    }

    public CamInstance findById(String code) {
        CamInstance camInstance = instanceMap.get(code);
        if (camInstance != null) {
            return camInstance;
        }
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(code);
        if (apExternalSystem.getType() == ApExternalSystemType.CAM) {
            camInstance = new CamInstance(apExternalSystem.getUrl(), apExternalSystem.getApiKeyId(), apExternalSystem.getApiKeyValue());
            instanceMap.put(apExternalSystem.getCode(), camInstance);
            return camInstance;
        }
        return null;
    }

    private SearchApi getSearchApiByCode(String code) {
        return getByCode(code).getSearchApi();
    }

    private EntityApi getEntityApiByCode(String code) {
        return getByCode(code).getEntityApi();
    }

    private ExportApi getExportApiByCode(String code) {
        return getByCode(code).getExportApi();
    }

    private BatchUpdatesApi getBatchUpdatesApiByCode(String code) {
        return getByCode(code).getBatchUpdatesApi();
    }

    private UpdatesApi getUpdatesApiByCode(String code) {
        return getByCode(code).getUpdatesApi();
    }
}
