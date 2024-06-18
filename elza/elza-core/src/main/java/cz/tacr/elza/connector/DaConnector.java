package cz.tacr.elza.connector;

import com.lightcomp.ft.wsdl.v1.FileTransferService;
import cz.tacr.da.ApiException;
import cz.tacr.da.controller.DefaultApi;
import cz.tacr.da.controller.vo.DownloadDownloadAips;
import cz.tacr.da.controller.vo.DownloadDownloadStatus;
import cz.tacr.da.controller.vo.UpdatedAips;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

    public void invalidate(ArrDigitalRepository digitalRepository) {
        instanceMap.remove(digitalRepository.getExternalSystemId());
    }

    private DefaultApi getDefaultApi(ArrDigitalRepository digitalRepository) {
        return get(digitalRepository).getDefaultApi();
    }

    private FileTransferService getFileTransferService(ArrDigitalRepository digitalRepository) {
        return get(digitalRepository).getFileTransferService();
    }

    public DaInstance get(ArrDigitalRepository digitalRepository) {
        if (digitalRepository.getDigitalRepositoryType() == DigitalRepositoryType.WSDL ||
                digitalRepository.getDigitalRepositoryType() == DigitalRepositoryType.FILESYSTEM) {
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
