package cz.tacr.elza.service.report;

import java.util.List;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.domain.RptParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

public class ReportSysMonthUserCount extends ReportBase {

	public ReportSysMonthUserCount(String code, String query, List<RptParam> params, ReportService reportService, EntityManager em) {
		super(code, query, params, reportService, em);
	}

	/**
	 * Přehled po měsících dle uživatelů
	 * Přehled všech změn za dané období a uživatele po měsících. 
	 * Součástí přehledu jsou jednotky popisu, prvky popisu a archivní entity 
     *
	 * @param parameters
	 * @return ReportReportData
	 */
	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		validateAndReadParameters(parameters);
		reportService.checkAndUpdateViews(reportCode);

		Query query = em.createNativeQuery(reportQuery, Tuple.class);
		query.setParameter("dateFrom", dateFrom.toLocalDate());
		query.setParameter("dateTo", dateTo.toLocalDate());

		// do dotazu vložíme parametry - data jako text
		// z nějakého důvodu standardní náhrada tohoto parametru v H2 nefunguje
		if (reportService.getDatasourceUrl().contains("jdbc:h2")) {
			String sysMonthUserCountQuery = reportQuery
					.replace(":dateFrom", "'" + dateFrom.toLocalDate().toString() + "'")
					.replace(":dateTo", "'" + dateTo.toLocalDate().toString() + "'");
			query = em.createNativeQuery(sysMonthUserCountQuery, Tuple.class);
		}

		List<Tuple> result = query.getResultList();

		return createReportData(result);
	}
}
