package cz.tacr.elza.connector;

import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.DownloadRequest;
import com.lightcomp.ft.client.Transfer;
import com.lightcomp.ft.client.UploadRequest;
import com.lightcomp.ft.simple.DwnldRequestImpl;
import com.lightcomp.ft.simple.UploadRequestImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public byte[] downloadDownload(ArrDigitalRepository digitalRepository, String batchId) throws ApiException {
        return getDefaultApi(digitalRepository).downloadDownload(batchId);
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

    public Transfer ingestFileTransfer(ArrDigitalRepository digitalRepository, Path exportDir) {
        GenericDataType genericDataType = new GenericDataType();
        genericDataType.setId(UUID.randomUUID().toString());
        genericDataType.setType("ingest");
        UploadRequest uploadRequest = new UploadRequestImpl(exportDir, genericDataType);
        return getFileTransferClient(digitalRepository).upload(uploadRequest);
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
