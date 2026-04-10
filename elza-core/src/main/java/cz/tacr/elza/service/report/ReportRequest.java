package cz.tacr.elza.service.report;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.RequestProcessState;

public class ReportRequest {

    final private Integer userId;

    final private Integer requestId;

    final private ReportProcessor processor;
    
    final private ReportReportParameters reportParameters;

    private RequestProcessState state = RequestProcessState.PENDING;

    private Exception exception;

    private ReportReportData reportData;

	public ReportRequest(Integer userId, Integer requestId, ReportProcessor processor, ReportReportParameters reportParameters) {
		this.userId = userId;
		this.requestId = requestId;
		this.processor = processor;
		this.reportParameters = reportParameters;
	}

	public Integer getUserId() {
		return userId;
	}

	public Integer getRequestId() {
		return requestId;
	}

	public ReportProcessor getProcessor() {
		return processor;
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
