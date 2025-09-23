package cz.tacr.elza.cam.v1;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.validation.Schema;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static cz.tacr.elza.cam.v1.CamException.prepareExtSystemException; 

import cz.tacr.cam.v1.client.ApiException;
import cz.tacr.cam.v1.client.ApiResponse;
import cz.tacr.cam.v1.client.controller.EntityApi;
import cz.tacr.cam.v1.client.controller.ExportApi;
import cz.tacr.cam.v1.client.controller.SearchApi;
import cz.tacr.cam.v1.client.controller.UpdatesApi;
import cz.tacr.cam.v1.client.controller.vo.QueryParamsDef;
import cz.tacr.cam.v1.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateXml;
import cz.tacr.cam.v1.schema.cam.EntitiesXml;
import cz.tacr.cam.v1.schema.cam.EntityXml;
import cz.tacr.cam.v1.schema.cam.QueryResultXml;
import cz.tacr.cam.v1.schema.cam.UpdatesFromXml;
import cz.tacr.cam.v1.schema.cam.UpdatesXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ApiCamConnector;
import cz.tacr.elza.cam.JaxbUtils;
import cz.tacr.elza.cam.ProcessingContext;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SyncImpossibleException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

@Service
public class CamConnector implements ApiCamConnector {

    private static final Logger logger = LoggerFactory.getLogger(CamConnector.class);

    public static final String APIKEY_ID = "apiKeyId";
    public static final String APIKEY_VALUE = "apiKeyValue";

    @Autowired
    private SchemaManager schemaManager;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private SearchFilterFactory searchFilterFactory;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private CamService camService;

    /**
     * External system ID to CamInstance map
     */
    private final Map<Integer, CamInstance> instanceMap = new HashMap<>();

	@Override
    public String getDetailUrl(ApExternalSystem extSystem) {
		CamInstance canInstance = get(extSystem);
        return canInstance.getDetailtUrl();
    }

	@Override
	public ArchiveEntityResultListVO search(int from, int max, SearchFilterVO filter, ApExternalSystem extlSystem) {
        QueryResultXml result;
        try {
            result = search(from + 1, max, searchFilterFactory.createQueryParamsDef(filter), extlSystem);
        } catch (ApiException e) {
            throw prepareExtSystemException(e);
        }
        return searchFilterFactory.createArchiveEntityVoListResult(result);
	}

	@Override
	public Integer takeArchiveEntity(String archiveEntityId, Integer scopeId, ApExternalSystem extSystem) {
        EntityXml entity;
        try {
            entity = getEntity(archiveEntityId, extSystem);
        } catch (ApiException e) {
            throw prepareExtSystemException(e);
        }

        ApBinding binding = externalSystemService.findByValueAndExternalSystem(archiveEntityId, extSystem);
        if (binding != null) {
            // check state
            Optional<ApBindingState> bindingState = externalSystemService.getBindingState(binding);
            bindingState.ifPresent(bs -> {
                throw new SystemException("Archival entity already imported", ExternalCode.ALREADY_IMPORTED)
                        .set("externalSystemCode", extSystem.getCode())
                        .set("archiveEntityId", archiveEntityId)
                        .set("bindingStateId", bs.getBindingStateId())
                        .set("accessPointId", bs.getAccessPointId());
            });
        }

        ApScope scope = accessPointService.getApScope(scopeId);
        ProcessingContext procCtx = new ProcessingContext(scope, extSystem, staticDataService);
        List<ApState> apStates = camService.takeAccessPoints(procCtx, Collections.singletonList(entity));
        if (apStates.size() != 1) {
            throw new BusinessException("Failed to create accesspoint from entity", BaseCode.IMPORT_FAILED);
        }

        ApState apState = apStates.get(0);
        return apState.getAccessPointId();
	}

	@Override
	public void takeRelArchiveEntities(ApState state, ApExternalSystem extSystem) {
        List<String> archiveEntities = accessPointService.findRelArchiveEntities(state.getAccessPoint());
        List<EntityXml> entities = new ArrayList<>();

        try {
            if (CollectionUtils.isNotEmpty(archiveEntities)) {
                for (String archiveEntityId : archiveEntities) {
                    entities.add(getEntity(archiveEntityId, extSystem));
                }
            }
        } catch (ApiException e) {
            throw prepareExtSystemException(e);
        }
        ProcessingContext procCtx = new ProcessingContext(state.getScope(), extSystem, staticDataService);
        camService.takeAccessPoints(procCtx, entities);
	}

	@Override
	public void synchronizeEntity(ApState state, ApBindingState bindingState, ApExternalSystem extSystem) {
        EntityXml entity;
        try {
            entity = getEntity(bindingState.getBinding().getValue(), extSystem);
        } catch (ApiException e) {
            throw prepareExtSystemException(e);
        }
        ProcessingContext procCtx = new ProcessingContext(state.getScope(), extSystem, staticDataService);
        try {
            camService.synchronizeAccessPoint(procCtx, bindingState.getBinding(), entity, false);
        } catch (SyncImpossibleException e) {
            logger.error("Synchronized impossible, accessPointId: {}, bindingId: {}, {}", state.getAccessPointId(), bindingState.getBindingId(), e.getMessage());
            throw new BusinessException("Synchronizace této entity s CAM není možná. " + e.getMessage(),
                    e,
                    ExternalCode.SYNC_IMPOSSIBLE);
        }
	}

	@Override
	public void connectArchiveEntity(String archiveEntityId, ApState state, ApExternalSystem extSystem, Boolean replace) {
        EntityXml entity;
        try {
            entity = getEntity(archiveEntityId, extSystem);
        } catch (ApiException e) {
            throw prepareExtSystemException(e);
        }
        
        ProcessingContext procCtx = new ProcessingContext(state.getScope(), extSystem, staticDataService);

        camService.connectAccessPoint(state, entity, procCtx, replace);
	}

	public QueryResultXml search(final int page,
                                 final int pageSize,
                                 final QueryParamsDef query,
                                 final ApExternalSystem apExternalSystem) throws ApiException {
        ApiResponse<File> fileApiResponse = getSearchApi(apExternalSystem).searchApsWithHttpInfo(page, pageSize, query);
        return unmarshal(QueryResultXml.class, fileApiResponse);
    }

    public EntityXml getEntity(final String archiveEntityId,
                               final Integer externalSystemId) throws ApiException {
        ApiResponse<File> fileApiResponse = getEntityApi(externalSystemId).getEntityByIdWithHttpInfo(archiveEntityId);
        return unmarshal(EntityXml.class, fileApiResponse);
    }

    public EntityXml getEntity(final String archiveEntityId,
                               final ApExternalSystem externalSystem) throws ApiException {
        ApiResponse<File> fileApiResponse = getEntityApi(externalSystem).getEntityByIdWithHttpInfo(archiveEntityId);
        return unmarshal(EntityXml.class, fileApiResponse);
    }

    public EntitiesXml getEntities(final List<String> archiveEntityIds,
                                   final Integer externalSystemId) throws ApiException {
        ApiResponse<File> fileApiResponse = getExportApi(externalSystemId).exportSnapshotsWithHttpInfo(archiveEntityIds);
        return unmarshal(EntitiesXml.class, fileApiResponse);
    }

    public EntitiesXml getEntities(final List<String> archiveEntityIds,
                                   final ApExternalSystem externalSystem) throws ApiException {
        ApiResponse<File> fileApiResponse = getExportApi(externalSystem).exportSnapshotsWithHttpInfo(archiveEntityIds);
        return unmarshal(EntitiesXml.class, fileApiResponse);
    }

    public BatchUpdateResultXml postNewBatch(final BatchUpdateXml batchUpdate,
                                             final ApExternalSystem externalSystem,
                                             final String apikeyId, final String apikeyValue) throws ApiException {
        Schema schema = schemaManager.getSchema(SchemaManager.CAM_SCHEMA_URL);
        File xmlFile = JaxbUtils.asFile(batchUpdate, schema);
        
        if(logger.isDebugEnabled()) {
        	// log file content if needed
        	byte[] encoded;
			try {
				encoded = Files.readAllBytes(xmlFile.toPath());
				String data = new String(encoded, "utf-8");
                if (apikeyId != null) {
                    logger.debug("postNewBatch: Sending data to {} as {}: {}", externalSystem.getName(), apikeyId,
                                 data);
                } else {
                    logger.debug("postNewBatch: Sending data to {}: {}", externalSystem.getName(), data);
                }
			} catch (IOException e) {
                logger.error("postNewBatch: Failed to log data", e);
			}        	
        }
        try {
            ApiResponse<File> fileApiResponse = get(externalSystem, apikeyId, apikeyValue)
                .getBatchUpdatesApi()
                .postNewBatchWithHttpInfo(xmlFile);
            return unmarshal(BatchUpdateResultXml.class, fileApiResponse);
        } finally {
            xmlFile.delete();
        }
    }

    public BatchUpdateResultXml getBatchStatus(final String bid,
                                               final ApExternalSystem externalSystem) throws ApiException {
        ApiResponse<File> fileApiResponse = get(externalSystem).getBatchUpdatesApi().getBatchStatusWithHttpInfo(bid);
        return unmarshal(BatchUpdateResultXml.class, fileApiResponse);
    }

    public UpdatesFromXml getUpdatesFrom(final String fromTransId,
                                         final Integer externalSystemId) throws ApiException {
        ApiResponse<File> fileApiResponse = getUpdatesApi(externalSystemId).getUpdatesFromWithHttpInfo(fromTransId);
        return unmarshal(UpdatesFromXml.class, fileApiResponse);
    }

    public UpdatesXml getUpdatesFromTo(final String fromTransId,
                                       final String toTransId,
                                       final Integer page,
                                       final Integer pageSize,
                                       final Integer externalSystemId) throws ApiException {
        ApiResponse<File> fileApiResponse = getUpdatesApi(externalSystemId).getUpdatesFromToWithHttpInfo(fromTransId, toTransId, page, pageSize);
        return unmarshal(UpdatesXml.class, fileApiResponse);
    }

    /**
     * Invalidate external
     * 
     * @param apExternalSystem
     */
    public void invalidate(ApExternalSystem apExternalSystem) {
        if (apExternalSystem.getType() == ApExternalSystemType.CAM ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_UUID ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
            instanceMap.remove(apExternalSystem.getExternalSystemId());
        }
    }

    public CamInstance get(Integer apExternalSystemId) {
        return get(externalSystemService.getExternalSystemInternal(apExternalSystemId));
    }

    public CamInstance get(ApExternalSystem apExternalSystem) {
    	return get(apExternalSystem, null, null);
    }

    public CamInstance get(ApExternalSystem apExternalSystem, String apikeyId, String apikeyValue) {
        if (apExternalSystem.getType() == ApExternalSystemType.CAM ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_UUID ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
        	// if apikeyId & apikeyValue define - use its
        	if (apikeyId != null && apikeyValue != null) {
                return new CamInstance(apExternalSystem.getUrl(), apikeyId, apikeyValue);
        	}
        	// use cache instanceMap
            CamInstance camInstance = instanceMap.get(apExternalSystem.getExternalSystemId());
            if (camInstance == null) {
                camInstance = new CamInstance(apExternalSystem.getUrl(), apExternalSystem.getApiKeyId(), apExternalSystem.getApiKeyValue());
                instanceMap.put(apExternalSystem.getExternalSystemId(), camInstance);
            }
            return camInstance;
        } else {
            throw new IllegalArgumentException("Externí systém není typu CAM");
        }
    }

    private <T> T unmarshal(final Class<T> classObject, final ApiResponse<File> apiResponse) {
        if (logger.isDebugEnabled()) {
            logger.debug("Unmarshalling received data ({}), statusCode: {}", classObject.getName(),
                         apiResponse.getStatusCode());
        }
        try (InputStream in = new FileInputStream(apiResponse.getData())) {
            JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(in);
        } catch (Exception e) {
            throw new SystemException("Nepodařilo se načíst objekt " + classObject.getSimpleName() + " ze streamu", e, PackageCode.PARSE_ERROR).set("class", classObject.toString());
        } finally {
            apiResponse.getData().delete();
        }
    }

    private SearchApi getSearchApi(ApExternalSystem apExternalSystem) {
        return get(apExternalSystem).getSearchApi();
    }

    private EntityApi getEntityApi(ApExternalSystem apExternalSystem) {
        return get(apExternalSystem).getEntityApi();
    }

    private EntityApi getEntityApi(Integer apExternalSystemId) {
        return get(apExternalSystemId).getEntityApi();
    }

    private ExportApi getExportApi(ApExternalSystem apExternalSystem) {
        return get(apExternalSystem).getExportApi();
    }

    private ExportApi getExportApi(Integer apExternalSystemId) {
        return get(apExternalSystemId).getExportApi();
    }

    private UpdatesApi getUpdatesApi(Integer apExternalSystemId) {
        return get(apExternalSystemId).getUpdatesApi();
    }
}
