package cz.tacr.elza.service.report;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.controller.vo.ReportReportCategory;
import cz.tacr.elza.controller.vo.ReportReportData;
import cz.tacr.elza.controller.vo.ReportReportDefinition;
import cz.tacr.elza.controller.vo.ReportReportParamDefinition;
import cz.tacr.elza.controller.vo.ReportReportParameters;
import cz.tacr.elza.controller.vo.ReportReportRow;
import cz.tacr.elza.controller.vo.ReportValue;
import cz.tacr.elza.controller.vo.ReportValueAccesspointId;
import cz.tacr.elza.controller.vo.ReportValueDate;
import cz.tacr.elza.controller.vo.ReportValueFondId;
import cz.tacr.elza.controller.vo.ReportValueInteger;
import cz.tacr.elza.controller.vo.ReportValueString;
import cz.tacr.elza.controller.vo.ReportValueType;
import cz.tacr.elza.domain.RptParam;
import cz.tacr.elza.domain.RptRequiredView;
import cz.tacr.elza.domain.RptValueType;
import cz.tacr.elza.domain.RptViewDate;
import cz.tacr.elza.domain.SysViewUpdate;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.RptCategoryRepository;
import cz.tacr.elza.repository.RptParamRepository;
import cz.tacr.elza.repository.RptValueTypeRepository;
import cz.tacr.elza.repository.RptViewDateRepository;
import cz.tacr.elza.repository.SysViewUpdateRepository;
import cz.tacr.elza.repository.RptReportRepository;
import cz.tacr.elza.repository.RptRequiredViewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Service
public class ReportService {

	public static final String RT_SYS_TOTAL_COUNT = "SYS_TOTAL_COUNT";

	public static final String RT_SYS_MONTH_USER_COUNT = "SYS_MONTH_USER_COUNT";

	public static final String RT_SYS_INSTITUTION_COUNT = "SYS_INSTITUTION_COUNT";

	private final List<String> SYS_TOTAL_COUNT_HEADERS = List.of("as_pocet", "jp_pocet", "pp_pocet", "ae_pocet", "pb_pocet", "vpb_pocet");

	private final List<String> SYS_INSTITUTION_COUNT_HEADERS = List.of("internal_code", "string_value", "fonds_cnt", "levels_cnt", "items_cnt", "refents_cnt");

	private final List<String> MONTH_USER_COUNT_HEADERS = List.of("date_year", "date_month", "username", "level_new", "level_delete", 
			"item_new", "item_update", "item_delete", "ap_new", "ap_update", "ap_delete", "ap_replace", "apusg_new", "apusg_delete");

	private final String VIEW_NODE_CHANGE = "rpt_view_node_change";

	private final String VIEW_ITEM_CHANGE = "rpt_view_item_change";

	private final String VIEW_AP_USAGE = "rpt_view_ap_usage";

	private final String VIEW_AP_CHANGE = "rpt_view_ap_change";
	
	// maximální "zastarávání" dat v hodinách
	private final int HOURS_TO_REFRESH = 2;

	@Autowired
	EntityManager em;
	
	@Autowired
	PlatformTransactionManager txManager;
	
	@Autowired
	RptCategoryRepository categoryRepository;

	@Autowired
	RptReportRepository reportRepository;
	
	@Autowired
	RptParamRepository paramRepository;

	@Autowired
	RptValueTypeRepository paramTypeRepository;

	@Autowired
	RptRequiredViewRepository requiredViewRepository;

	@Autowired
	RptViewDateRepository viewDateRepository;

	@Autowired
	SysViewUpdateRepository viewUpdateRepository;

	@Autowired
	ApChangeRepository apChangeRepository;

	@Autowired
	ChangeRepository arrChangeRepository;

	/**
	 * Získání seznamu všech statistických sestav
	 * 
	 * @return seznam
	 */
	public List<ReportReportCategory> getDefinitions() {
		List<ReportReportCategory> result = new ArrayList<>();
		Map<Integer, ReportReportCategory> resultMap = new HashMap<>();
		
		categoryRepository.findAll().forEach(c -> {
			ReportReportCategory rrc = new ReportReportCategory(c.getCode(), c.getName(), new ArrayList<>());
			result.add(rrc);
			resultMap.put(c.getCategoryId(), rrc);
		});
		
		Map<Integer, ReportReportDefinition> definitionsMap = new HashMap<>();
		reportRepository.findAll().forEach(rpt -> {
			ReportReportDefinition definition = new ReportReportDefinition(rpt.getCode(), rpt.getName());
			resultMap.get(rpt.getCategoryId()).getReportDefinitions().add(definition);
			definitionsMap.put(rpt.getReportId(), definition);
		});
		
		
		List<RptValueType> paramTypes = paramTypeRepository.findAll();

		Map<Integer, RptValueType> paramTypesMap = paramTypes
				.stream()
				.collect(Collectors.toMap(RptValueType::getValueTypeId, Function.identity()));

		List<RptParam> rptParams = paramRepository.findAll();
		rptParams.forEach(rptParam -> {
			// get definition
			ReportReportDefinition definition = definitionsMap.get(rptParam.getReportId());
			// add parameter
			// TODO: prepare default values
			ReportReportParamDefinition paramDef = new ReportReportParamDefinition(
					rptParam.getCode(), rptParam.getName(), 
					ReportValueType.valueOf(paramTypesMap.get(rptParam.getValueTypeId()).getCode()), 
					rptParam.getRequired(), rptParam.getRepeatable());
			definition.addParamsItem(paramDef);
		});

		return result;
	}

	/**
	 * Vytváření statistické sestavy
	 * 
	 * @param request
	 * @return
	 */
	public ReportReportData createReport(ReportRequest request) {
		switch (request.getCode()) {
		case RT_SYS_TOTAL_COUNT:
			return reportSysTotalCount();
		case RT_SYS_MONTH_USER_COUNT:
			return reportMonthUserCount(request.getReportParameters());
		case RT_SYS_INSTITUTION_COUNT:
			return reportSysInstitutionCount(request.getReportParameters());
		default:
			throw new IllegalArgumentException("Unexpected value: " + request.getCode());
		}
	}

	/**
	 * Souhrnné informace – aktuální stav
	 * Souhrnný přehled počtu archivních souborů, jednotek popisu a archivní entit.
	 * 
	 * @return
	 */
	private ReportReportData reportSysTotalCount() {
		checkAndUpdateViews(RT_SYS_TOTAL_COUNT);

		Query query = em.createNativeQuery(ReportServiceQuery.SYS_TOTAL_COUNT_QUERY);
		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(SYS_TOTAL_COUNT_HEADERS);

		ReportReportRow row = new ReportReportRow();
		for (Object item : result.get(0)) {
			row.addColsItem(new ReportValueInteger(((Long)item).intValue(), ReportValueType.INT));
		}
		reportData.addRowsItem(row);
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}

	/**
	 * Přehled po měsících dle uživatelů
	 * Přehled všech změn za dané období a uživatele po měsících. 
	 * Součástí přehledu jsou jednotky popisu, prvky popisu a archivní entity 
	 * 
	 * @return
	 */
	private ReportReportData reportMonthUserCount(ReportReportParameters parameters) {
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

		checkAndUpdateViews(RT_SYS_MONTH_USER_COUNT);

		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(MONTH_USER_COUNT_HEADERS);

		for (Object[] item : result) {
			ReportReportRow row = new ReportReportRow();
			row.addColsItem(new ReportValueInteger(((Integer)item[0]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Integer)item[1]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueString((String)item[2], ReportValueType.STRING));
			row.addColsItem(new ReportValueInteger(((Long)item[3]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[4]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[5]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[6]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[7]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[8]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[9]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[10]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[11]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[12]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[13]).intValue(), ReportValueType.INT));
			reportData.addRowsItem(row);
		}
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}

	/**
	 * Přehled k datu dle institucí
	 * Souhn počtu archivních souborů, jednotek popisu a prvků popisu k danému datu.
	 * 
	 * @param request
	 * @return
	 */
	private ReportReportData reportSysInstitutionCount(ReportReportParameters parameters) {
		Query query;
		if (parameters == null || parameters.getParams() == null || parameters.getParams().isEmpty()) {
			query = em.createNativeQuery(ReportServiceQuery.SYS_INSTITUTION_COUNT_QUERY);
		} else {

			// TODO zkontrolovat parametr

			OffsetDateTime changeDate = ((ReportValueDate)parameters.getParams().get(0).getValues().get(0)).getDateValue();

			query = em.createNativeQuery(ReportServiceQuery.SYS_INSTITUTION_COUNT_WITH_DATE_QUERY);
			query.setParameter("changeDate", changeDate);
		}

		checkAndUpdateViews(RT_SYS_INSTITUTION_COUNT);

		List<Object[]> result = query.getResultList();

		ReportReportData reportData = new ReportReportData();
		reportData.setHeader(SYS_INSTITUTION_COUNT_HEADERS);

		for (Object[] item : result) {
			ReportReportRow row = new ReportReportRow();
			row.addColsItem(new ReportValueString((String)item[0], ReportValueType.STRING));
			row.addColsItem(new ReportValueString((String)item[1], ReportValueType.STRING));
			row.addColsItem(new ReportValueInteger(((Long)item[2]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[3]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[4]).intValue(), ReportValueType.INT));
			row.addColsItem(new ReportValueInteger(((Long)item[5]).intValue(), ReportValueType.INT));
			reportData.addRowsItem(row);
		}
		reportData.setSourceDataDate(OffsetDateTime.now());

		return reportData;
	}

	/**
	 * Kontrola a v případě potřeby aktualizace dat
	 * 
	 * @param reportCode
	 */
	private void checkAndUpdateViews(String reportCode) {
		// získáme seznam `view` pro kontrolu relevance dat
		List<RptRequiredView> views = requiredViewRepository.findByReportCode(reportCode);

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		updateRptViewDate(transactionTemplate);

		// aktualizace views
		SysViewUpdate viewUpdate;
		for (RptRequiredView view : views) {
			switch (view.getViewName()) {
			case VIEW_NODE_CHANGE:
				viewUpdate = checkRefreshNeeds(VIEW_NODE_CHANGE);
				if (viewUpdate.getLastRefresh() == null) {
					transactionTemplate.executeWithoutResult(ts -> {
						Query query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_NODE_CHANGE_INSERT);
						query.executeUpdate();
						query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_NODE_CHANGE_DELETE);
						query.executeUpdate();
					});
					viewUpdate.setLastRefresh(OffsetDateTime.now());
					viewUpdateRepository.save(viewUpdate);
				}
				break;
			case VIEW_ITEM_CHANGE:
				viewUpdate = checkRefreshNeeds(VIEW_ITEM_CHANGE);
				if (viewUpdate.getLastRefresh() == null) {
					transactionTemplate.executeWithoutResult(ts -> {
						Query query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_ITEM_CHANGE_INSERT);
						query.executeUpdate();
						query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_ITEM_CHANGE_DELETE);
						query.executeUpdate();
					});
					viewUpdate.setLastRefresh(OffsetDateTime.now());
					viewUpdateRepository.save(viewUpdate);
				}
				break;
			case VIEW_AP_USAGE:
				viewUpdate = checkRefreshNeeds(VIEW_AP_USAGE);
				if (viewUpdate.getLastRefresh() == null) {
					transactionTemplate.executeWithoutResult(ts -> {
						Query query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_AP_USAGE_INSERT);
						query.executeUpdate();
						query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_AP_USAGE_UPDATE);
						query.executeUpdate();
						query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_AP_USAGE_DELETE);
						query.executeUpdate();
					});
					viewUpdate.setLastRefresh(OffsetDateTime.now());
					viewUpdateRepository.save(viewUpdate);
				}
				break;
			case VIEW_AP_CHANGE:
				viewUpdate = checkRefreshNeeds(VIEW_AP_CHANGE);
				if (viewUpdate.getLastRefresh() == null) {
					transactionTemplate.executeWithoutResult(ts -> {
						Query query = em.createNativeQuery(ReportServiceQuery.UPDATE_VIEW_AP_CHANGE_INSERT);
						query.executeUpdate();
					});
					viewUpdate.setLastRefresh(OffsetDateTime.now());
					viewUpdateRepository.save(viewUpdate);
				}
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + view.getViewName());
			}
		}
	}

	/**
	 * Aktualizace tabulky rpt_view_date
	 * 
	 * @param transactionTemplate
	 */
	private void updateRptViewDate(TransactionTemplate transactionTemplate) {
		transactionTemplate.executeWithoutResult(ts -> {
			// generovat data do konce aktuálního roku
			LocalDate dateTo = LocalDate.of(LocalDate.now().getYear(), 12, 31);
			RptViewDate dt = viewDateRepository.findById(dateTo).orElse(null);
			if (dt == null) {
				// určit počáteční datum generování záznamů
				LocalDate dateFrom;
				RptViewDate viewDateLast = viewDateRepository.findTop1ByOrderByDateIdDesc();
				if (viewDateLast == null) {
					OffsetDateTime apFrom = apChangeRepository.findTop1ByOrderByChangeIdAsc().getChangeDate();
					OffsetDateTime arrFrom = arrChangeRepository.findTop1ByOrderByChangeIdAsc().getChangeDate();
					OffsetDateTime dateTimeFrom = apFrom.isBefore(arrFrom) ? apFrom : arrFrom;
					dateFrom = dateTimeFrom.toLocalDate();
				} else {
					dateFrom = viewDateLast.getDateId().plusDays(1);
				}
	
				List<RptViewDate> dates = new LinkedList<>();
				for (LocalDate date = dateFrom; !date.isAfter(dateTo); date = date.plusDays(1)) {
					dt = new RptViewDate();
					dt.setDateId(date);
					dt.setYear(date.getYear());
					dt.setQuarter((date.getMonthValue() - 1) / 3 + 1);
					dt.setMonth(date.getMonthValue());
					dt.setDay(date.getDayOfMonth());
					dt.setDayOfWeek(date.getDayOfWeek().getValue());
					dt.setIsHolyday(false); // TODO vyřešit problém s identifikací svátků
					dates.add(dt);
				}
				viewDateRepository.saveAll(dates);
			}
		});
	}

	/**
	 * Kontrola potřeby aktualizace
	 * 
	 * @param viewName
	 * @return SysViewUpdate.lastUpdate == null - pokud potřebujeme aktualizovat
	 */
	private SysViewUpdate checkRefreshNeeds(String viewName) {
		SysViewUpdate viewUpdate = viewUpdateRepository.findByViewName(viewName);
		if (viewUpdate == null) {
			viewUpdate = new SysViewUpdate();
			viewUpdate.setViewName(viewName);
			return viewUpdate;
		}

		// před jak dlouhou dobou byla data aktualizována?
		Duration duration = Duration.between(viewUpdate.getLastRefresh(), OffsetDateTime.now());
		if (duration.toHours() > HOURS_TO_REFRESH) {
			viewUpdate.setLastRefresh(null);
		}

		return viewUpdate;
	}

	/**
	 * Generování sestavy ve formátu VSD
	 * 
	 * @param reportData
	 * @return
	 */
	public String getCsvReport(ReportReportData reportData) {
		StringBuilder sb = new StringBuilder();
		for (String header: reportData.getHeader()) {
			if (!sb.isEmpty()) {
				sb.append(",");
			}
			sb.append(header);
		}
		for (ReportReportRow row : reportData.getRows()) {
			StringBuilder sbr = new StringBuilder();
			for (ReportValue value : row.getCols()) {
				if (!sbr.isEmpty()) {
					sbr.append(",");
				}
				switch (value.getValueType()) {
				case INT:
					sbr.append(((ReportValueInteger) value).getIntValue());
					break;
				case STRING:
					sbr.append(((ReportValueString) value).getTextValue());
					break;
				case DATE:
					sbr.append(((ReportValueDate) value).getDateValue());
					break;
				case AP_ID:
					sbr.append(((ReportValueAccesspointId) value).getAccesspointId());
					break;
				case FUND_ID:
					sbr.append(((ReportValueFondId) value).getFondId());
					break;
				default:
					throw new IllegalArgumentException("Unexpected type: " + value.getValueType());
				}
			}
			sb.append("\n" + sbr.toString());
		}
		return sb.toString();
	}

}
