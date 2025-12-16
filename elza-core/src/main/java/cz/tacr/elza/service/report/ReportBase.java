package cz.tacr.elza.service.report;

import java.sql.Date;
import java.time.Instant;
import java.time.OffsetDateTime;
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
import cz.tacr.elza.domain.RptDefaultValueGenerator;
import cz.tacr.elza.domain.RptParam;
import cz.tacr.elza.domain.RptValueType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

public abstract class ReportBase implements ReportProcessor {

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

	/**
	 * Vytvoření dotazu s parametry
	 * 
	 * @param parameters
	 * @return
	 */
	protected Query createQuery(ReportReportParameters parameters) {
		Query query = em.createNativeQuery(reportQuery, Tuple.class);

		if (CollectionUtils.isEmpty(reportParams)) {
			return query;
		}

		for (RptParam param : reportParams) {
			String paramCode = param.getCode();
			RptValueType rptValueType = reportService.getValueType(param.getValueTypeId());
			ReportValueType valueType = ReportValueType.valueOf(rptValueType.getCode());
			Object paramValue = getOrGeneratedValue(paramCode, valueType, param.getGenerator(), param.getRequired(), parameters);
			if (reportQuery.contains(':' + paramCode)) {
				query.setParameter(paramCode, paramValue);
			} else {
				throw new IllegalArgumentException("Required parameter not found in query, paramCode: " + paramCode);
			}
		}

		return query;
	}

	/**
	 * Získání nebo generování hodnoty parametru
	 * 
	 * @param paramCode
	 * @param valueType
	 * @param required
	 * @param parameters
	 * @return
	 */
	protected Object getOrGeneratedValue(String paramCode, ReportValueType valueType, RptDefaultValueGenerator generator, boolean required, ReportReportParameters parameters) {
		List<ReportValue> values = getValuesByCode(paramCode, parameters);
		if (CollectionUtils.isEmpty(values)) {
			if (required) {
				throw new IllegalArgumentException("Required parameter value(s) not found, code: " + paramCode);
			}
			values = generateParamValues(valueType, generator);
		}
		return getValueFromReportValue(values, generator);
	}

	/**
	 * Získání hodnoty parametru podle kódu
	 * 
	 * @param code
	 * @param parameters
	 * @return
	 */
	private List<ReportValue> getValuesByCode(String code, ReportReportParameters parameters) {
		for (ReportReportParamValue value : parameters.getParams()) {
			if (code.equals(value.getCode())) {
				return value.getValues();
			}
		}
		return null;
	}

	/**
	 * Získání hodnoty parametru pro dotaz
	 * 
	 * @param values
	 * @return
	 */
	private Object getValueFromReportValue(List<ReportValue> values, RptDefaultValueGenerator generator) {
		// TODO now we use only first value
		ReportValue value = values.get(0);
		switch (value.getValueType()) {
		case DATE:
			OffsetDateTime dateValue = ((ReportValueDate) value).getDateValue();
			// pokud existuje generátor, bereme čas z generátoru
			if (generator != null) {
				OffsetDateTime dt = (OffsetDateTime) generator.getDefaultValue();
				dateValue = dateValue.withHour(dt.getHour()).withMinute(dt.getMinute()).withSecond(dt.getSecond()).withNano(dt.getNano());
			}
			return dateValue;
		case STRING:
			return ((ReportValueString) value).getTextValue();
		default:
			throw new IllegalArgumentException("Unexpected value type: " + value.getValueType());
		}
	}

	/**
	 * Vytvoření třídy sestavy s daty z dotazu
	 * 
	 * @param result
	 * @param lastRefresh
	 * @return
	 */
	protected ReportReportData createReportData(List<Tuple> result, OffsetDateTime lastRefresh) {
		ReportReportData reportData = new ReportReportData();
		if (!CollectionUtils.isEmpty(result)) {
			reportData.setHeader(result.get(0).getElements().stream().map(i -> i.getAlias().toUpperCase()).toList());
			reportData.setRows(getReportRows(result));
		}
		reportData.setSourceDataDate(lastRefresh);

		return reportData;
	}

	/**
	 * Generování chybějících hodnot parametrů
	 * 
	 * @param param
	 * @return
	 */
	protected List<ReportValue> generateParamValues(ReportValueType type, RptDefaultValueGenerator generator) {
		List<ReportValue> values = new ArrayList<>();
		ReportValue value = null;
		switch (type) {
		case DATE:
			value = new ReportValueDate((OffsetDateTime) generator.getDefaultValue(), ReportValueType.DATE);
			break;
		case STRING:
			value = new ReportValueString(null, ReportValueType.STRING); // TODO dočasné řešení
			break;
		default:
			throw new IllegalArgumentException("Unexpected value type valueTypeId: " + type);
		}
		values.add(value);

		return values;
	}

	/**
	 * Konverze dat z dotazu
	 * 
	 * @param result
	 * @return list of ReportReportRow
	 */
	protected List<ReportReportRow> getReportRows(List<Tuple> result) {
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
				} else if (type == Date.class) {
					reportRow.addColsItem(new ReportValueString(((Date) row.get(element)).toString(), ReportValueType.STRING));
				} else if (type == Instant.class) {
					reportRow.addColsItem(new ReportValueString(((Instant) row.get(element)).toString(), ReportValueType.STRING));
				} else if (type == Object.class) {
					reportRow.addColsItem(new ReportValueString("", ReportValueType.STRING)); // prázdná hodnota
				} else {
					throw new IllegalArgumentException("Unexpected type: " + type);
				}
			}
			reportRows.add(reportRow);
		}
		return reportRows;
	}
}
