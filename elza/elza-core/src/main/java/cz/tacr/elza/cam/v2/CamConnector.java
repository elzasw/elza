package cz.tacr.elza.cam.v2;

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
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static cz.tacr.elza.cam.v2.CamException.prepareExtSystemException;

import cz.tacr.cam.v2.client.ApiException;
import cz.tacr.cam.v2.client.ApiResponse;
import cz.tacr.cam.v2.client.controller.EntityApi;
import cz.tacr.cam.v2.client.controller.ExportApi;
import cz.tacr.cam.v2.client.controller.SearchApi;
import cz.tacr.cam.v2.client.controller.UpdatesApi;
import cz.tacr.cam.v2.client.controller.vo.BatchUpdateStatus;
import cz.tacr.cam.v2.client.controller.vo.ExportRequestStatus;
import cz.tacr.cam.v2.client.controller.vo.QueryParamsDef;
import cz.tacr.cam.v2.client.controller.vo.RequestProcessState;
import cz.tacr.cam.v2.client.controller.vo.SearchType;
import cz.tacr.cam.v2.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v2.schema.cam.EntitiesXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.cam.v2.schema.cam.QueryResultXml;
import cz.tacr.cam.v2.schema.cam.UpdatesFromXml;
import cz.tacr.cam.v2.schema.cam.UpdatesXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ApiCamConnector;
import cz.tacr.elza.cam.ProcessingContext;
import cz.tacr.elza.controller.vo.ArchiveEntityResultListVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SyncImpossibleException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

@Service("camConnectorV2")
public class CamConnector implements ApiCamConnector {

    private static final Logger logger = LoggerFactory.getLogger(CamConnector.class);

    public static final String APIKEY_ID = "apiKeyId";
    public static final String APIKEY_VALUE = "apiKeyValue";

    private static final int MAX_EXPORT_BATCH_SIZE = 1000;
    private static final long EXPORT_POLL_INTERVAL_MS = 1000L;
    private static final long EXPORT_TIMEOUT_MS = 5L * 60L * 1000L;

    @Autowired
    private AccessPointConnectorService apConnectService;
    
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
	public void synchronizeAccessPointsForExternalSystem(String extSysCode) {
		camService.synchronizeAccessPointsForExternalSystem(extSysCode);
	}

	@Override
    public String getDetailUrl(ApExternalSystem extSystem) {
		CamInstance canInstance = get(extSystem);
        return canInstance.getDetailtUrl();
    }

	@Override
	public ArchiveEntityResultListVO search(int from, int max, SearchFilterVO filter, ApExternalSystem extlSystem) {
        QueryResultXml result;
        try {
            result = search(from + 1, max, searchFilterFactory.createQueryParamsDef(filter), null, extlSystem);
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

	@Override
	public void exportApForce(ExtSyncsQueueItem queueItem) {
		try {
			UUID uuid = camService.upload(queueItem, queueItem.getData(), null);
			apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_PROCESSING, null, uuid.toString(), queueItem.getData(), null);
		} catch (ApiException e) {
			logger.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
			apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.ERROR, CamException.getApiExceptionInfo(e));
			throw new BusinessException("Failed to synchronize ELZA -> CAM.", e, BaseCode.INVALID_STATE);
		}
	}

	public QueryResultXml search(final int page,
                                 final int pageSize,
                                 final QueryParamsDef query,
                                 final SearchType searchType,
                                 final ApExternalSystem apExternalSystem) throws ApiException {
        ApiResponse<File> fileApiResponse = getSearchApi(apExternalSystem).searchEntitiesWithHttpInfo(page, pageSize, query, searchType);
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
        return exportEntities(archiveEntityIds, getExportApi(externalSystemId), getEntityApi(externalSystemId));
    }

    public EntitiesXml getEntities(final List<String> archiveEntityIds,
                                   final ApExternalSystem externalSystem) throws ApiException {
        return exportEntities(archiveEntityIds, getExportApi(externalSystem), getEntityApi(externalSystem));
    }

    private EntitiesXml exportEntities(final List<String> archiveEntityIds,
                                       final ExportApi exportApi,
                                       final EntityApi entityApi) throws ApiException {
        if (CollectionUtils.isEmpty(archiveEntityIds)) {
            return new EntitiesXml();
        }
        EntitiesXml merged = new EntitiesXml();
        for (List<String> chunk : ListUtils.partition(archiveEntityIds, MAX_EXPORT_BATCH_SIZE)) {
            EntitiesXml part;
            try {
                part = exportChunk(exportApi, chunk);
            } catch (ApiException | SystemException e) {
                if (!isFallbackEligible(e)) {
                    throw e;
                }
                logger.warn("CAM v2 bulk export failed, falling back to per-entity download; chunkSize: {}",
                            chunk.size(), e);
                part = fetchEntitiesIndividually(entityApi, chunk);
            }
            if (part.getEntity() != null) {
                merged.getEntity().addAll(part.getEntity());
            }
        }
        return merged;
    }

    private EntitiesXml fetchEntitiesIndividually(final EntityApi entityApi,
                                                  final List<String> archiveEntityIds) throws ApiException {
        EntitiesXml result = new EntitiesXml();
        for (String id : archiveEntityIds) {
            try {
                ApiResponse<File> resp = entityApi.getEntityByIdWithHttpInfo(id);
                EntityXml entity = unmarshal(EntityXml.class, resp);
                result.getEntity().add(entity);
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    logger.warn("CAM v2 fallback: entity not found, skipping; id: {}", id);
                    continue;
                }
                throw e;
            }
        }
        return result;
    }

    private static boolean isFallbackEligible(final Throwable e) {
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }
        if (e instanceof ApiException) {
            int code = ((ApiException) e).getCode();
            return code == 0 || code >= 500;
        }
        return e instanceof SystemException;
    }

    private EntitiesXml exportChunk(final ExportApi exportApi,
                                    final List<String> chunk) throws ApiException {
        String requestId = exportApi.exportSnapshots(chunk);
        logger.info("CAM v2 export started, requestId: {}, size: {}", requestId, chunk.size());

        awaitExportFinished(exportApi, requestId);

        ApiResponse<File> fileApiResponse = exportApi.downloadExportWithHttpInfo(requestId);
        EntitiesXml result = unmarshal(EntitiesXml.class, fileApiResponse);
        logger.info("CAM v2 export downloaded, requestId: {}", requestId);
        return result;
    }

    private void awaitExportFinished(final ExportApi exportApi,
                                     final String requestId) throws ApiException {
        long deadline = System.currentTimeMillis() + EXPORT_TIMEOUT_MS;
        while (true) {
            ExportRequestStatus status = exportApi.getExportStatus(requestId);
            RequestProcessState state = status.getState();
            logger.debug("CAM v2 export status, requestId: {}, state: {}, progress: {}",
                         requestId, state, status.getProgress());

            if (state == RequestProcessState.FINISHED) {
                return;
            }
            if (state == RequestProcessState.ERROR) {
                throw new SystemException("CAM v2 export skončil chybou", PackageCode.PARSE_ERROR)
                        .set("requestId", requestId);
            }
            if (System.currentTimeMillis() > deadline) {
                throw new SystemException("CAM v2 export nedokončen v časovém limitu", PackageCode.PARSE_ERROR)
                        .set("requestId", requestId)
                        .set("state", state != null ? state.getValue() : null)
                        .set("timeoutMs", EXPORT_TIMEOUT_MS);
            }
            try {
                Thread.sleep(EXPORT_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SystemException("CAM v2 export přerušen", e, PackageCode.PARSE_ERROR)
                        .set("requestId", requestId);
            }
        }
    }

    public UUID postNewBatch(final String batchUpdate,
                             final ApExternalSystem externalSystem,
                             final String apikeyId, 
                             final String apikeyValue,
                             @Nullable final Boolean force,
                             @Nullable final String forceKey) throws ApiException {
    	// log file content if needed
        if (logger.isDebugEnabled()) {
            if (apikeyId != null) {
                logger.debug("postNewBatch: Sending data to {} as {}: {}", externalSystem.getName(), apikeyId, batchUpdate);
            } else {
                logger.debug("postNewBatch: Sending data to {}: {}", externalSystem.getName(), batchUpdate);
            }
        }

        File xmlFile = null;
        try {
        	xmlFile = File.createTempFile("cam-", ".api.xml");        
        	Files.writeString(xmlFile.toPath(), batchUpdate);
        } catch (IOException e) {
        	logger.error("Failed to write XML file: {}" + xmlFile.getAbsolutePath(), e);
            throw new SystemException("Nepodařilo se uložit batchUpdate do souboru", e, 
            		BaseCode.EXPORT_FAILED).set("xmlFile", xmlFile.getAbsolutePath());
        }

        try {
            ApiResponse<UUID> uuidApiResponse = get(externalSystem, apikeyId, apikeyValue)
                .getBatchUpdatesApi()
                .postNewBatchWithHttpInfo(xmlFile, force, forceKey);
            return uuidApiResponse.getData();
        } finally {
            xmlFile.delete();
        }
    }

    public BatchUpdateStatus getBatchStatus(final UUID updateRequestId,
                                            final ApExternalSystem externalSystem) throws ApiException {
        ApiResponse<BatchUpdateStatus> fileApiResponse = get(externalSystem).getBatchUpdatesApi().getBatchStatusWithHttpInfo(updateRequestId);
        return fileApiResponse.getData();
    }

    public BatchUpdateResultXml getBatchResult(final UUID updateRequestId,
                                               final ApExternalSystem externalSystem) throws ApiException {
    	ApiResponse<File> fileApiResponse = get(externalSystem).getBatchUpdatesApi().getBatchResultWithHttpInfo(updateRequestId);
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
        if (apExternalSystem.getType() == ApExternalSystemType.CAM_V2 ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_UUID_V2 ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE_V2) {
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
        if (apExternalSystem.getType() == ApExternalSystemType.CAM_V2 ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_UUID_V2 ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE_V2) {
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
