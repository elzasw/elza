package cz.tacr.elza.service.report;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportReportRow;
import cz.tacr.elza.controller.vo.ReportValueInteger;
import cz.tacr.elza.controller.vo.ReportValueString;
import cz.tacr.elza.controller.vo.ReportValueType;
import cz.tacr.elza.domain.RptParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

public class ReportBase implements ReportProcessor {

	protected final String reportCode;

	protected final String reportQuery;

	protected final List<RptParam> reportParams; 

	protected final ReportService reportService;

	protected final EntityManager em;

	public ReportBase(String code, String query, List<RptParam> params, ReportService reportService, EntityManager em) {
		this.reportCode = code;
		this.reportQuery = query;
		this.reportParams = params;
		this.reportService = reportService;
		this.em = em;
	}

	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		reportService.checkAndUpdateViews(reportCode);

		Query query = em.createNativeQuery(reportQuery, Tuple.class);
		List<Tuple> result = query.getResultList();

		return createReportData(result);
	}

	/**
	 * Vytvoření třídy sestavy s daty z dotazu
	 * 
	 * @param result
	 * @return
	 */
	protected ReportReportData createReportData(List<Tuple> result) {
		ReportReportData reportData = new ReportReportData();
		if (!CollectionUtils.isEmpty(result)) {
			reportData.setHeader(result.get(0).getElements().stream().map(i -> i.getAlias().toUpperCase()).toList());
			reportData.setRows(fillReportRows(result));
		}
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}

	/**
	 * Konverze dat z dotazu
	 * 
	 * @param result
	 * @return list of ReportReportRow
	 */
	protected List<ReportReportRow> fillReportRows(List<Tuple> result) {
		List<ReportReportRow> reportRows = new ArrayList<>();
		for (Tuple row : result) {
			ReportReportRow reportRow = new ReportReportRow();
			for (TupleElement<?> element : row.getElements()) {
				Class<?> type = element.getJavaType();
				if (type == String.class) {
					reportRow.addColsItem(new ReportValueString((String) row.get(element), ReportValueType.STRING));
				} else if (type == Integer.class) {
					reportRow.addColsItem(new ReportValueInteger((Integer) row.get(element), ReportValueType.INT));
				} else if (type == Long.class) {
					reportRow.addColsItem(new ReportValueInteger(((Long) row.get(element)).intValue(), ReportValueType.INT));
				} else {
					throw new IllegalArgumentException("Unexpected type: " + type);
				}
			}
			reportRows.add(reportRow);
		}
		return reportRows;
	}
}
