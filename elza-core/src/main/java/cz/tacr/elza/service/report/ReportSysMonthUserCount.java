package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportValueDate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class ReportSysMonthUserCount extends ReportBase {

	public static final String REPORT_NAME = "SYS_MONTH_USER_COUNT";

	private final List<String> REPORT_HEADERS = List.of("date_year", "date_month", "username", "level_new", "level_delete", 
			"item_new", "item_update", "item_delete", "ap_new", "ap_update", "ap_delete", "ap_replace", "apusg_new", "apusg_delete");

	public ReportSysMonthUserCount(EntityManager em, ReportService reportService) {
		super(em, reportService);
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
		String sysMonthUserCountQuery = ReportServiceQuery.SYS_MONTH_USER_COUNT_QUERY
				.replace(":fromDate", "'" + dateFrom.toLocalDate().toString() + "'")
				.replace(":toDate", "'" + dateTo.toLocalDate().toString() + "'");

		Query query = em.createNativeQuery(sysMonthUserCountQuery);

		reportService.checkAndUpdateViews(REPORT_NAME);

		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(REPORT_HEADERS);
		reportData.setRows(reportService.getReportRows(result));
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}
}
