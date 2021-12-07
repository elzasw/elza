package cz.tacr.elza.connector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;

import cz.tacr.cam.schema.cam.UpdatesFromXml;
import cz.tacr.cam.schema.cam.UpdatesXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.client.ApiResponse;
import cz.tacr.cam.client.controller.BatchUpdatesApi;
import cz.tacr.cam.client.controller.EntityApi;
import cz.tacr.cam.client.controller.ExportApi;
import cz.tacr.cam.client.controller.SearchApi;
import cz.tacr.cam.client.controller.UpdatesApi;
import cz.tacr.cam.client.controller.vo.QueryParamsDef;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.QueryResultXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.service.ExternalSystemService;

@Service
public class CamConnector {

    private static final Logger logger = LoggerFactory.getLogger(CamConnector.class);

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private ApExternalSystemRepository apExternalSystemRepository;

    @Autowired
    private SchemaManager schemaManager;

    /**
     * External system ID to CamInstance map
     */
    private final Map<Integer, CamInstance> instanceMap = new HashMap<>();

    public QueryResultXml search(final int page,
                                 final int pageSize,
                                 final QueryParamsDef query,
                                 final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getSearchApiByCode(externalSystemCode).searchApsWithHttpInfo(page, pageSize, query);
        return unmarshal(QueryResultXml.class, fileApiResponse);
    }

    public EntityXml getEntityById(final String archiveEntityId,
                                   final ApExternalSystem externalSystem) throws ApiException {
        ApiResponse<File> fileApiResponse = get(externalSystem).getEntityApi().getEntityByIdWithHttpInfo(archiveEntityId);
        return unmarshal(EntityXml.class, fileApiResponse);
    }

    public EntityXml getEntityById(final String archiveEntityId,
                                   final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getEntityApiByCode(externalSystemCode).getEntityByIdWithHttpInfo(archiveEntityId);
        return unmarshal(EntityXml.class, fileApiResponse);
    }

    public EntitiesXml getEntitiesByIds(final List<String> archiveEntityIds,
                                   final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getExportApiByCode(externalSystemCode).exportSnapshotsWithHttpInfo(archiveEntityIds);
        return unmarshal(EntitiesXml.class, fileApiResponse);
    }

    public BatchUpdateResultXml postNewBatch(final BatchUpdateXml batchUpdate,
                                             final ApExternalSystem externalSystem) throws ApiException {
        Schema schema = schemaManager.getSchema(SchemaManager.CAM_SCHEMA_URL);
        File xmlFile = JaxbUtils.asFile(batchUpdate, schema);
        
        if(logger.isDebugEnabled()) {
        	// log file content if needed
        	byte[] encoded;
			try {
				encoded = Files.readAllBytes(xmlFile.toPath());
				String data = new String(encoded, "utf-8");
				logger.debug("Sending data: {}", data);
			} catch (IOException e) {
				logger.error("Failed to log data", e);
			}        	
        }
        try {
            ApiResponse<File> fileApiResponse = get(externalSystem)
                .getBatchUpdatesApi()
                .postNewBatchWithHttpInfo(xmlFile);
            return unmarshal(BatchUpdateResultXml.class, fileApiResponse);
        } finally {
            xmlFile.delete();
        }
    }

    public BatchUpdateResultXml getBatchStatus(final String bid,
                                            final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getBatchUpdatesApiByCode(externalSystemCode).getBatchStatusWithHttpInfo(bid);
        return unmarshal(BatchUpdateResultXml.class, fileApiResponse);
    }

    public UpdatesFromXml getUpdatesFrom(final String fromTransId,
                                         final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getUpdatesApiByCode(externalSystemCode).getUpdatesFromWithHttpInfo(fromTransId);
        return unmarshal(UpdatesFromXml.class, fileApiResponse);
    }

    public UpdatesXml getUpdatesFromTo(final String fromTransId,
                                       final String toTransId,
                                       final Integer page,
                                       final Integer pageSize,
                                       final String externalSystemCode) throws ApiException {
        ApiResponse<File> fileApiResponse = getUpdatesApiByCode(externalSystemCode).getUpdatesFromToWithHttpInfo(fromTransId, toTransId, page, pageSize);
        return unmarshal(UpdatesXml.class, fileApiResponse);
    }

    /**
     * Invalidate external
     * 
     * @param code
     *            Might be code of none existing system
     */
    public void invalidate(String code) {
        ApExternalSystem apExternalSystem = apExternalSystemRepository.findByCode(code);
        if (apExternalSystem != null &&
                (apExternalSystem.getType() == ApExternalSystemType.CAM ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_UUID ||
                        apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE)) {
            instanceMap.remove(apExternalSystem.getExternalSystemId());
        }
    }

    public CamInstance get(ApExternalSystem apExternalSystem) {

        if (apExternalSystem.getType() == ApExternalSystemType.CAM ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_UUID ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
            CamInstance camInstance = instanceMap.get(apExternalSystem.getExternalSystemId());
            if (camInstance == null) {
                camInstance = new CamInstance(apExternalSystem.getUrl(), apExternalSystem.getApiKeyId(),
                        apExternalSystem.getApiKeyValue());
                instanceMap.put(apExternalSystem.getExternalSystemId(), camInstance);
            }
            return camInstance;
        } else {
            throw new IllegalArgumentException("Externí systém není typu CAM");
        }
    }

    public CamInstance getByCode(String code) {
        try {
            ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(code);
            return get(apExternalSystem);
        } catch (Exception e) {
            throw new IllegalArgumentException("Externí systém nenalezen");
        }
    }

    // TODO: rework to use id and not code
    public CamInstance findById(String code) {
    	ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(code);
        CamInstance camInstance = instanceMap.get(apExternalSystem.getExternalSystemId());
        if (camInstance != null) {
            return camInstance;
        }
        if (apExternalSystem.getType() == ApExternalSystemType.CAM ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_UUID ||
                apExternalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
            camInstance = new CamInstance(apExternalSystem.getUrl(), apExternalSystem.getApiKeyId(), apExternalSystem.getApiKeyValue());
            instanceMap.put(apExternalSystem.getExternalSystemId(), camInstance);
            return camInstance;
        }
        return null;
    }

    private <T> T unmarshal(final Class<T> classObject, final ApiResponse<File> apiResponse) {
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
