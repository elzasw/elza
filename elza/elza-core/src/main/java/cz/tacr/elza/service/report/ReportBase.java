package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.domain.RptParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class ReportBase implements ReportProcessor {

	protected final String reportCode;

	protected final List<String> reportHeaders;

	protected final String reportQuery;

	protected final List<RptParam> reportParams; 

	protected final ReportService reportService;

	protected final EntityManager em;

	public ReportBase(String code, List<String> headers, String query, List<RptParam> params, ReportService reportService, EntityManager em) {
		this.reportCode = code;
		this.reportHeaders = headers;
		this.reportQuery = query;
		this.reportParams = params;
		this.reportService = reportService;
		this.em = em;
	}

	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		reportService.checkAndUpdateViews(reportCode);

		Query query = em.createNativeQuery(reportQuery);
		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(reportHeaders);
		reportData.setRows(reportService.getReportRows(result));
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}

}
