package cz.tacr.elza.asynchactions;

import org.apache.commons.lang3.NotImplementedException;

import cz.tacr.elza.domain.ArrAsyncRequest;

/**
 * Zapouzdřený požadavek na zpracování
 *
 */
public class AsyncRequest extends AsyncRequestBase {

    private Integer bulkActionId;
    private Integer outputId;
    private Integer accessPointId;
    private Integer userId;
    private Integer exportId;
    private Integer aipActionItemId;

    public AsyncRequest(ArrAsyncRequest request) {
        super(request.getAsyncRequestId(),
                request.getPriority(),
                request.getFundVersion() != null ? request.getFundVersion().getFundVersionId() : null,
                request.getType());
        switch (type) {
            case BULK:
                bulkActionId = request.getBulkAction().getBulkActionRunId();
                break;
            case OUTPUT:
                outputId = request.getOutput().getOutputId();
                userId = request.getUserId();
                break;
            case AP:
                accessPointId = request.getAccessPoint().getAccessPointId();
                break;
            case EXPORT:
            	exportId = request.getExport().getExportId();
                userId = request.getUserId();
            	break;
            case AIP:
                aipActionItemId = request.getAipActionItem().getAipActionItemId();
                userId = request.getUserId();
                break;
            default:
                throw new NotImplementedException("Neimplmentovaný typ: " + type);
        }
    }

    public Integer getCurrentId() {
        switch (type) {
            case OUTPUT:
                return outputId;
            case BULK:
                return bulkActionId;
            case AP:
                return accessPointId;
            case EXPORT:
            	return exportId;
            case AIP:
                return aipActionItemId;
            default:
                throw new NotImplementedException("Neimplmentovaný typ: " + type);
        }
    }

    public Long getRequestId() {
        return requestId;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public Integer getBulkActionId() {
        return bulkActionId;
    }

    public Integer getOutputId() {
        return outputId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public Integer getExportId() {
        return exportId;
    }

    public Integer getAipActionItemId() {
        return aipActionItemId;
    }

    @Override
    public String toString() {
        return getType() + "(" + getCurrentId() + ") p=" + priority + ", fv=" + fundVersionId + ", reqId=" + requestId;
    }
}
