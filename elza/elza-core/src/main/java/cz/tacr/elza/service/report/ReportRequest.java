package cz.tacr.elza.service.report;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.RequestProcessState;

public class ReportRequest {

    final private Integer userId;

    final private Integer requestId;

    final private String code;
    
    final private ReportReportParameters reportParameters;

    private RequestProcessState state = RequestProcessState.PENDING;

    private Exception exception;

    private ReportReportData reportData;

	public ReportRequest(Integer userId, Integer requestId, String code, ReportReportParameters reportParameters) {
		this.userId = userId;
		this.requestId = requestId;
		this.code = code;
		this.reportParameters = reportParameters;
	}

	public Integer getUserId() {
		return userId;
	}

	public Integer getRequestId() {
		return requestId;
	}

	public String getCode() {
		return code;
	}

	public ReportReportParameters getReportParameters() {
		return reportParameters;
	}

	public RequestProcessState getState() {
		return state;
	}

	public void setState(RequestProcessState state) {
		this.state = state;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public ReportReportData getReportData() {
		return reportData;
	}

	public void setReportData(ReportReportData reportData) {
		this.reportData = reportData;
	}
}
