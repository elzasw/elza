package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportValueDate;
import cz.tacr.elza.domain.RptParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

public class ReportSysInstitutionCount extends ReportBase {

	public ReportSysInstitutionCount(String code, String query, List<RptParam> params, ReportService reportService, EntityManager em) {
		super(code, query, params, reportService, em);
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
			query = em.createNativeQuery(reportQuery, Tuple.class);
		} else {

			// TODO zkontrolovat parametr

			OffsetDateTime changeDate = ((ReportValueDate)parameters.getParams().get(0).getValues().get(0)).getDateValue();

			query = em.createNativeQuery(ReportServiceQuery.SYS_INSTITUTION_COUNT_WITH_DATE_QUERY, Tuple.class);
			query.setParameter("changeDate", changeDate);
		}

		reportService.checkAndUpdateViews(reportCode);

		List<Tuple> result = query.getResultList();

		return createReportData(result);
	}
}
