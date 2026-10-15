package cz.tacr.elza.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.DownloadRequest;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;

/**
 * File Transfer settings of a DA repository: the endpoint and the type of a download request.
 */
public class DaConnectorFileTransferTest {

    @Test
    void resolveFileTransferUrl_derivedFromApiUrl() {
        assertEquals("http://da:8096/ws/ft", DaInstance.resolveFileTransferUrl("http://da:8096", null));
        assertEquals("http://da:8096/ws/ft", DaInstance.resolveFileTransferUrl("http://da:8096", "  "));
    }

    @Test
    void resolveFileTransferUrl_trailingSlashOfApiUrlIsStripped() {
        assertEquals("http://da:8096/ws/ft", DaInstance.resolveFileTransferUrl(" http://da:8096/ ", null));
    }

    @Test
    void resolveFileTransferUrl_configuredUrlWins() {
        assertEquals("https://ft.example/transfer",
                DaInstance.resolveFileTransferUrl("http://da:8096", " https://ft.example/transfer "));
    }

    @Test
    void downloadFileTransfer_sendsConfiguredType() {
        GenericDataType data = download("GetMetadata_ELZA");

        assertEquals("batch-1", data.getId());
        assertEquals("GetMetadata_ELZA", data.getType());
    }

    @Test
    void downloadFileTransfer_withoutTypeSendsBatchIdOnly() {
        GenericDataType data = download(null);

        assertEquals("batch-1", data.getId());
        assertNull(data.getType());
    }

    private static GenericDataType download(String fileTransferType) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setFileTransferType(fileTransferType);

        Client client = mock(Client.class);
        DaInstance daInstance = mock(DaInstance.class);
        when(daInstance.getFileTransferClient()).thenReturn(client);
        DaConnector connector = spy(new DaConnector());
        doReturn(daInstance).when(connector).get(any());

        connector.downloadFileTransfer(repository, "batch-1", Path.of("download"));

        ArgumentCaptor<DownloadRequest> request = ArgumentCaptor.forClass(DownloadRequest.class);
        verify(client).downloadSync(request.capture());
        return request.getValue().getData();
    }
}
