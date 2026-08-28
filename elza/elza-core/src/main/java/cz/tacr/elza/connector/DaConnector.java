package cz.tacr.elza.connector;

import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.DownloadRequest;
import com.lightcomp.ft.client.Transfer;
import com.lightcomp.ft.simple.DwnldRequestImpl;
import com.lightcomp.ft.xsd.v1.GenericDataType;
import cz.tacr.da.ApiException;
import cz.tacr.da.controller.DefaultApi;
import cz.tacr.da.controller.vo.DownloadDownloadAips;
import cz.tacr.da.controller.vo.DownloadDownloadStatus;
import cz.tacr.da.controller.vo.IngestIngestResult;
import cz.tacr.da.controller.vo.IngestIngestStatus;
import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.service.da.vo.DaUploadRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import cz.tacr.elza.api.DaDownloadMethod;
import cz.tacr.elza.controller.vo.DigitalRepositoryTestResult;
import org.apache.commons.lang3.StringUtils;
import cz.tacr.elza.common.io.SpooledContent;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Service
public class DaConnector {

    private static final Logger logger = LoggerFactory.getLogger(DaConnector.class);

    public static final int FILE_TRANSFER_ERROR_CODE = 413;

    private final Map<Integer, DaInstance> instanceMap = new HashMap<>();

    public UpdatedAips updates(ArrDigitalRepository digitalRepository, Integer pageSize, String query) {
        try {
            return getDefaultApi(digitalRepository).updates(pageSize, query);
        } catch (ApiException e) {
            throw new IllegalStateException("Došlo k chybě při volání DA", e);
        }
    }

    public String downloadAips(ArrDigitalRepository digitalRepository, DownloadDownloadAips downloadDownloadAips) {
        try {
            return getDefaultApi(digitalRepository).downloadDownloadAips(downloadDownloadAips);
        } catch (ApiException e) {
            throw new IllegalStateException("Došlo k chybě při volání DA", e);
        }
    }

    public DownloadDownloadStatus downloadStatus(ArrDigitalRepository digitalRepository, String batchId) {
        try {
            return getDefaultApi(digitalRepository).downloadGeStatus(batchId);
        } catch (ApiException e) {
            throw new IllegalStateException("Došlo k chybě při volání DA", e);
        }
    }

    /**
     * Downloads the prepared batch over the DA API. The response body is streamed into
     * {@link SpooledContent}, so a large package never has to fit into the heap; the caller
     * closes the returned content.
     *
     * @throws ApiException with the HTTP status code when the DA refuses the download
     *                      (413 = too large, use File Transfer)
     */
    public SpooledContent downloadDownload(ArrDigitalRepository digitalRepository, String batchId) throws ApiException {
        okhttp3.Call call = getDefaultApi(digitalRepository).downloadDownloadCall(batchId, null);
        try (Response response = call.execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String errorBody = body == null ? null : body.string();
                throw new ApiException(response.message(), response.code(), response.headers().toMultimap(), errorBody);
            }
            if (body == null) {
                throw new ApiException("Empty response body when downloading batch " + batchId);
            }
            try (InputStream in = body.byteStream()) {
                return SpooledContent.readFrom(in);
            }
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    public void downloadFileTransfer(ArrDigitalRepository digitalRepository, String batchId, Path downloadDir) {
        GenericDataType genericDataType = new GenericDataType();
        genericDataType.setId(batchId);
        DownloadRequest downloadRequest = new DwnldRequestImpl(downloadDir, genericDataType);
        getFileTransferClient(digitalRepository).downloadSync(downloadRequest);
    }

    public IngestIngestStatus ingestStatus(ArrDigitalRepository digitalRepository, String batchId) {
        try {
            return getDefaultApi(digitalRepository).ingestGeStatus(batchId);
        } catch (ApiException e) {
            throw new IllegalStateException("Došlo k chybě při volání DA", e);
        }
    }

    public IngestIngestResult ingestResult(ArrDigitalRepository digitalRepository, String batchId) {
        try {
            return getDefaultApi(digitalRepository).ingestGetResult(batchId);
        } catch (ApiException e) {
            throw new IllegalStateException("Došlo k chybě při volání DA", e);
        }
    }

    public Transfer ingestFileTransfer(ArrDigitalRepository digitalRepository, DaUploadRequestImpl daUploadRequest) {
        return getFileTransferClient(digitalRepository).upload(daUploadRequest);
    }

    /**
     * Tests the configuration of a DA repository by calling the {@code /updates} operation of
     * the DA API with a fresh client built from the given (possibly not yet cached) settings.
     * The File Transfer endpoint has no probe operation and is not covered.
     */
    public DigitalRepositoryTestResult testRepository(ArrDigitalRepository digitalRepository) {
        DigitalRepositoryTestResult result = new DigitalRepositoryTestResult();
        result.setTemplated(false);
        result.setAvailable(false);

        if (digitalRepository.getDigitalRepositoryType() != DigitalRepositoryType.DA) {
            result.setMessage("Repository type " + digitalRepository.getDigitalRepositoryType()
                    + " is not a digital archive");
            return result;
        }
        if (StringUtils.isBlank(digitalRepository.getUrl())) {
            result.setMessage("Repository URL is not configured");
            return result;
        }

        DaInstance daInstance;
        try {
            daInstance = new DaInstance(digitalRepository.getUrl(),
                    digitalRepository.getApiKeyId(),
                    digitalRepository.getApiKeyValue(),
                    digitalRepository.getUsername(),
                    digitalRepository.getPassword());
        } catch (RuntimeException e) {
            result.setMessage("Invalid configuration: " + e.getMessage());
            return result;
        }
        result.setPath(daInstance.getApiUrl());
        try {
            UpdatedAips updates = daInstance.getDefaultApi().updates(1, null);
            result.setAvailable(true);
            StringBuilder message = new StringBuilder("DA API responds");
            if (updates != null && updates.getAipIds() != null && !updates.getAipIds().isEmpty()) {
                message.append("; first available AIP: ").append(updates.getAipIds().get(0).getAipId());
            } else {
                message.append("; no AIP available");
            }
            if (digitalRepository.getDownloadMethod() == DaDownloadMethod.FILE_TRANSFER) {
                message.append(". File Transfer endpoint ").append(daInstance.getFileTransferUrl())
                        .append(" is not covered by this test");
            }
            result.setMessage(message.toString());
        } catch (ApiException e) {
            result.setMessage("DA API call failed: HTTP " + e.getCode()
                    + (StringUtils.isBlank(e.getMessage()) ? "" : " - " + e.getMessage()));
        } catch (RuntimeException e) {
            result.setMessage("DA API call failed: " + e.getMessage());
        } finally {
            daInstance.stopFileTransferClient();
        }
        return result;
    }

    public void invalidate(ArrDigitalRepository digitalRepository) {
        DaInstance daInstance = instanceMap.remove(digitalRepository.getExternalSystemId());
        if (daInstance != null) {
            daInstance.stopFileTransferClient();
        }
    }

    private DefaultApi getDefaultApi(ArrDigitalRepository digitalRepository) {
        return get(digitalRepository).getDefaultApi();
    }

    private Client getFileTransferClient(ArrDigitalRepository digitalRepository) {
        return get(digitalRepository).getFileTransferClient();
    }

    public DaInstance get(ArrDigitalRepository digitalRepository) {
        if (digitalRepository.getDigitalRepositoryType() == DigitalRepositoryType.DA) {
            // use cache instanceMap
            DaInstance daInstance = instanceMap.get(digitalRepository.getExternalSystemId());
            if (daInstance == null) {
                daInstance = new DaInstance(digitalRepository.getUrl(),
                        digitalRepository.getApiKeyId(),
                        digitalRepository.getApiKeyValue(),
                        digitalRepository.getUsername(),
                        digitalRepository.getPassword());
                instanceMap.put(digitalRepository.getExternalSystemId(), daInstance);
            }
            return daInstance;
        } else {
            throw new IllegalArgumentException("Externí systém není typu DA");
        }
    }

}
