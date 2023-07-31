package cz.tacr.elza.dataexchange.output;

/**
 * Parameters for io export request.
 */
public class IOExportRequest extends DEExportParams {

    private Integer userId;

    private Integer requestId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }
}
