package cz.tacr.elza.service.report;

import java.util.List;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.domain.RptParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

public class ReportSimpleQuery extends ReportBase {

	public ReportSimpleQuery(String code, String query, List<RptParam> params, ReportService reportService, EntityManager em) {
		super(code, query, params, reportService, em);
	}

	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		reportService.checkAndUpdateViews(reportCode);

		Query query = createQuery(parameters);

		List<Tuple> result = query.getResultList();

		return createReportData(result);
	}

}
