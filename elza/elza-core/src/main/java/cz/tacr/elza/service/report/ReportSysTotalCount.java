package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.List;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public class ReportSysTotalCount extends ReportBase {

	public static final String REPORT_NAME = "SYS_TOTAL_COUNT";

	private final List<String> REPORT_HEADERS = List.of("as_pocet", "jp_pocet", "pp_pocet", "ae_pocet", "pb_pocet", "vpb_pocet");

	public ReportSysTotalCount(EntityManager em, ReportService reportService) {
		super(em, reportService);
	}

	/**
	 * Souhrnné informace – aktuální stav
	 * Souhrnný přehled počtu archivních souborů, jednotek popisu a archivní entit.
     *
	 * @param parameters
	 * @return ReportReportData
	 */
	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		reportService.checkAndUpdateViews(REPORT_NAME);

		Query query = em.createNativeQuery(ReportServiceQuery.SYS_TOTAL_COUNT_QUERY);
		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(REPORT_HEADERS);
		reportData.setRows(reportService.getReportRows(result));
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}
}
