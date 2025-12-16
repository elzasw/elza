package cz.tacr.elza.service.report;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;

public interface ReportProcessor {

	ReportReportData createReport(ReportReportParameters parameters);
}
