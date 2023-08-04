package cz.tacr.elza.dataexchange.output;

/**
 * Parameters for io export request.
 */
public class IOExportRequest {

    final private Integer userId;

    final private Integer requestId;

    /**
     * Recommended file name
     */
    final private String downloadFileName;

    private IOExportState state = IOExportState.PENDING;
    private Exception exception;

    final private DEExportParams exportParams;

    public IOExportRequest(final Integer userId,
                           final Integer requestId,
                           final String downloadFileName,
                           final DEExportParams exportParams) {
        this.userId = userId;
        this.requestId = requestId;
        this.downloadFileName = downloadFileName;
        this.exportParams = exportParams;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public DEExportParams getExportParams() {
        return exportParams;
    }

    public IOExportState getState() {
        return state;
    }

    public Exception getException() {
        return exception;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public void setStateProcessing() {
        state = IOExportState.PROCESSING;
    }

    public void setFinished() {
        state = IOExportState.FINISHED;
    }

    public void setFailed(final Exception exception) {
        this.exception = exception;
        state = IOExportState.ERROR;
    }
}
