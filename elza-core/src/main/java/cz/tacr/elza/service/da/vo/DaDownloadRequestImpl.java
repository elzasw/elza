package cz.tacr.elza.service.da.vo;

import java.nio.file.Path;

import com.lightcomp.ft.simple.DwnldRequestImpl;
import com.lightcomp.ft.xsd.v1.GenericDataType;

/**
 * Download request of a DA batch that remembers how its transfer ended.
 *
 * The File Transfer client reports the end of a transfer only through the callbacks of the
 * request: a synchronous download returns normally after a failed transfer as well, with the
 * failure written to the log of the client alone. The caller therefore asks the request whether
 * the batch was delivered.
 */
public class DaDownloadRequestImpl extends DwnldRequestImpl {

    private volatile boolean failed;

    private volatile boolean canceled;

    public DaDownloadRequestImpl(Path downloadDir, GenericDataType data) {
        super(downloadDir, data);
    }

    @Override
    public void onTransferFailed() {
        super.onTransferFailed();
        failed = true;
    }

    @Override
    public void onTransferCanceled() {
        super.onTransferCanceled();
        canceled = true;
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean isCanceled() {
        return canceled;
    }

    /**
     * @throws IllegalStateException when the transfer did not deliver the batch; the cause is in
     *                               the log of the File Transfer client (com.lightcomp.ft)
     */
    public void checkDelivered() {
        if (failed) {
            throw new IllegalStateException("Přenos dávky " + getData().getId()
                    + " přes File Transfer selhal, příčina je v logu serveru (com.lightcomp.ft)");
        }
        if (canceled) {
            throw new IllegalStateException("Přenos dávky " + getData().getId() + " přes File Transfer byl zrušen");
        }
    }
}
