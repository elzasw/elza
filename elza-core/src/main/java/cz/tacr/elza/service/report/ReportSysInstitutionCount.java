package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
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
		OffsetDateTime lastRefresh = reportService.checkAndUpdateViews(reportCode);

		Query query = (parameters != null && !CollectionUtils.isEmpty(parameters.getParams())) ? 
				createQuery(parameters) 
				: em.createNativeQuery(ReportServiceQuery.SYS_INSTITUTION_COUNT_QUERY, Tuple.class);

		List<Tuple> result = query.getResultList();

		return createReportData(result, lastRefresh);
	}
}
