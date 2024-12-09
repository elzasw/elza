package cz.tacr.elza.service.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportParamValue;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportReportRow;
import cz.tacr.elza.controller.vo.ReportValue;
import cz.tacr.elza.controller.vo.ReportValueDate;
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

	protected OffsetDateTime dateFrom;

	protected OffsetDateTime dateTo;

	public ReportBase(String code, String query, List<RptParam> params, ReportService reportService, EntityManager em) {
		this.reportCode = code;
		this.reportQuery = query;
		this.reportParams = params;
		this.reportService = reportService;
		this.em = em;
	}

	@Override
	public ReportReportData createReport(ReportReportParameters parameters) {
		validateAndReadParameters(parameters);
		reportService.checkAndUpdateViews(reportCode);

		Query query = em.createNativeQuery(reportQuery, Tuple.class);
		if (dateFrom != null) {
			query.setParameter("dateFrom", dateFrom);
		}
		if (dateTo != null) {
			query.setParameter("dateTo", dateTo);
		}

		List<Tuple> result = query.getResultList();

		return createReportData(result);
	}

	/**
	 * Kontrola a vyplňování polí parametrů
	 * 
	 * @param parameters
	 */
	protected void validateAndReadParameters(ReportReportParameters parameters) {
		// tato sestava nemá žádné parametry
		if (reportParams == null) {
			return;
		}
		
		for (RptParam param : reportParams) {
			String code = param.getCode();
			boolean required = param.getRequired();
			ReportValue value = getValueByCode(code, parameters);
			OffsetDateTime dtNow = OffsetDateTime.now();
			switch (code) {
			case "DATE_FROM":
				if (value == null && required) {
					throw new IllegalArgumentException("Required parameter value not found, code: " + code);
				}
				dateFrom = value == null ? dtNow.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC) : ((ReportValueDate) value).getDateValue();
				break;
			case "DATE_TO":
				if (value == null && required) {
					throw new IllegalArgumentException("Required parameter value not found, code: " + code);
				}
				dateTo = value == null ? dtNow : ((ReportValueDate) value).getDateValue();
				break;
			case "INSTITUCE":
				// TODO process this parameters
				break;
			default:
				throw new IllegalArgumentException("Unexpected param code: " + code);
			}
		}
	}

	/**
	 * Získání hodnoty parametru podle kódu
	 * 
	 * @param code
	 * @param parameters
	 * @return
	 */
	private ReportValue getValueByCode(String code, ReportReportParameters parameters) {
		for (ReportReportParamValue value : parameters.getParams()) {
			if (code.equals(value.getCode())) {
				return value.getValues().get(0);
			}
		}
		return null;
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
				} else if (type == Instant.class) {
					reportRow.addColsItem(new ReportValueString(((Instant) row.get(element)).atOffset(ZoneOffset.UTC).toString(), ReportValueType.STRING));
				} else if (type == Object.class) {
					reportRow.addColsItem(new ReportValue());
				} else {
					throw new IllegalArgumentException("Unexpected type: " + type);
				}
			}
			reportRows.add(reportRow);
		}
		return reportRows;
	}
}
