package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.nio.file.Path;

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

    protected final String mediaType;
    
    protected final String fileExt;

    protected final DEExportService exportService;

    public IOExportRequest(final Integer userId, 
    		               final Integer requestId, 
    		               final String downloadFileName, 
    		               final String mediaType, 
    		               final String fileExt,
    		               final DEExportService exportService) {
        this.userId = userId;
        this.requestId = requestId;
        this.downloadFileName = downloadFileName;
        this.mediaType = mediaType;
        this.fileExt = fileExt;
        this.exportService = exportService;
    }

    abstract void exportToFile(Path exportFile) throws IOException;

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

    public String getMediaType() {
		return mediaType;
	}

	public String getFileExt() {
		return fileExt;
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
