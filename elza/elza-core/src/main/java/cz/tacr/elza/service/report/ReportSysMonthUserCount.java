package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportValueDate;
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
		Objects.requireNonNull(parameters);
		Objects.requireNonNull(parameters.getParams());

		// TODO kontrola parametrů

		OffsetDateTime dateFrom = ((ReportValueDate)parameters.getParams().get(0).getValues().get(0)).getDateValue();
		OffsetDateTime dateTo = OffsetDateTime.now();
		if (parameters.getParams().size() == 2) {
			dateTo = ((ReportValueDate)parameters.getParams().get(1).getValues().get(0)).getDateValue();
		}

		// do dotazku vložíme parametry - data jako text
		// z nějakého důvodu standardní náhrada tohoto parametru nefunguje
		String sysMonthUserCountQuery = reportQuery
				.replace(":fromDate", "'" + dateFrom.toLocalDate().toString() + "'")
				.replace(":toDate", "'" + dateTo.toLocalDate().toString() + "'");

		Query query = em.createNativeQuery(sysMonthUserCountQuery, Tuple.class);

		reportService.checkAndUpdateViews(reportCode);

		List<Tuple> result = query.getResultList();

		return createReportData(result);
	}
}
