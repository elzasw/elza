package cz.tacr.elza.dataexchange.output;

/**
 * Parameters for io export request.
 */
public abstract class IOExportRequest {

    final protected Integer userId;

    final protected Integer requestId;

    /**
     * Recommended file name
     */
    final protected String downloadFileName;

    protected IOExportState state = IOExportState.PENDING;

    protected Exception exception;

    protected final String fileNameExt;

    public IOExportRequest(final Integer userId, final Integer requestId, final String downloadFileName, final String fileNameExt) {
        this.userId = userId;
        this.requestId = requestId;
        this.downloadFileName = downloadFileName;
        this.fileNameExt = fileNameExt;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getRequestId() {
        return requestId;
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

    public String getFileNameExt() {
		return fileNameExt;
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
