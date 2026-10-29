package cz.tacr.elza.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

    /** The DA communication standard names the download transfer {@code download}. */
    @Test
    void downloadFileTransfer_withoutTypeSendsStandardType() {
        GenericDataType data = download(null);

        assertEquals("batch-1", data.getId());
        assertEquals("download", data.getType());
        assertEquals("download", DaConnector.DEFAULT_FILE_TRANSFER_TYPE);
    }

    @Test
    void downloadFileTransfer_blankTypeSendsStandardType() {
        assertEquals("download", download("  ").getType());
    }

    /**
     * The client returns normally from a failed transfer and reports it through the request
     * only; a download that returned nothing must not pass as delivered.
     */
    @Test
    void downloadFileTransfer_failedTransferThrows() {
        Client client = mock(Client.class);
        doAnswer(inv -> {
            inv.getArgument(0, DownloadRequest.class).onTransferFailed();
            return null;
        }).when(client).downloadSync(any());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> download(client, "GetMetadata_ELZA"));
        assertTrue(e.getMessage().contains("batch-1"), e.getMessage());
        assertTrue(e.getMessage().contains("selhal"), e.getMessage());
    }

    @Test
    void downloadFileTransfer_canceledTransferThrows() {
        Client client = mock(Client.class);
        doAnswer(inv -> {
            inv.getArgument(0, DownloadRequest.class).onTransferCanceled();
            return null;
        }).when(client).downloadSync(any());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> download(client, "GetMetadata_ELZA"));
        assertTrue(e.getMessage().contains("zrušen"), e.getMessage());
    }

    private static GenericDataType download(String fileTransferType) {
        Client client = mock(Client.class);
        download(client, fileTransferType);

        ArgumentCaptor<DownloadRequest> request = ArgumentCaptor.forClass(DownloadRequest.class);
        verify(client).downloadSync(request.capture());
        return request.getValue().getData();
    }

    private static void download(Client client, String fileTransferType) {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setFileTransferType(fileTransferType);

        DaInstance daInstance = mock(DaInstance.class);
        when(daInstance.getFileTransferClient()).thenReturn(client);
        DaConnector connector = spy(new DaConnector());
        doReturn(daInstance).when(connector).get(any());

        connector.downloadFileTransfer(repository, "batch-1", Path.of("download"));
    }
}
