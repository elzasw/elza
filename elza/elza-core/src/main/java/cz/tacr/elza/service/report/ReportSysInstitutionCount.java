package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportValueDate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class ReportSysInstitutionCount extends ReportBase {

	public static final String REPORT_NAME = "SYS_INSTITUTION_COUNT";

	private final List<String> REPORT_HEADERS = List.of("internal_code", "string_value", "fonds_cnt", "levels_cnt", "items_cnt", "refents_cnt");

	public ReportSysInstitutionCount(EntityManager em, ReportService reportService) {
		super(em, reportService);
	}

	/**
	 * Přehled k datu dle institucí
	 * Souhn počtu archivních souborů, jednotek popisu a prvků popisu k danému datu.
	 * 
	 * @param parameters
	 * @return ReportReportData
	 */
	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		Query query;
		if (parameters == null || parameters.getParams() == null || parameters.getParams().isEmpty()) {
			query = em.createNativeQuery(ReportServiceQuery.SYS_INSTITUTION_COUNT_QUERY);
		} else {

			// TODO zkontrolovat parametr

			OffsetDateTime changeDate = ((ReportValueDate)parameters.getParams().get(0).getValues().get(0)).getDateValue();

			query = em.createNativeQuery(ReportServiceQuery.SYS_INSTITUTION_COUNT_WITH_DATE_QUERY);
			query.setParameter("changeDate", changeDate);
		}

		reportService.checkAndUpdateViews(REPORT_NAME);

		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(REPORT_HEADERS);
		reportData.setRows(reportService.getReportRows(result));
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}
}
