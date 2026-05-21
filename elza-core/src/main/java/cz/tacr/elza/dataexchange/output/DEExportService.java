package cz.tacr.elza.dataexchange.output;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportPhase;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.filters.AccessRestrictConfig;
import cz.tacr.elza.dataexchange.output.filters.ExportFilter;
import cz.tacr.elza.dataexchange.output.filters.ExportFilterConfig;
import cz.tacr.elza.dataexchange.output.filters.conditions.And;
import cz.tacr.elza.dataexchange.output.filters.conditions.EntityProperties;
import cz.tacr.elza.dataexchange.output.filters.conditions.Not;
import cz.tacr.elza.dataexchange.output.filters.conditions.PartCondition;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.ArrangementService.FindFundVersionsResult;
import cz.tacr.elza.service.cache.AccessPointCacheProvider;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.NodeCacheService;
import jakarta.persistence.EntityManager;

/**
 * Service for data-exchange export.
 */
@Service
public class DEExportService {

    private final static Logger log = LoggerFactory.getLogger(DEExportService.class);

    private final ExportInitHelper initHelper;

    private final ArrangementService arrangementService;

    private final StaticDataService staticDataService;

    private final InstitutionRepository institutionRepository;
    
    private final ApStateRepository stateRepository;

    private final ApIndexRepository indexRepository;

    private final ApItemRepository apItemRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final DataStringRepository dataStringRepository;

    private final ScopeRepository scopeRepository;

    private final ItemRepository itemRepository;

    private final NodeRepository nodeRepository;
    
    private final RuleService ruleService;

    private final ElzaLocale elzaLocale;

    @Autowired
    public DEExportService(EntityManager em,
		       			   UserService userService,
            		       LevelRepository levelRepository,
            		       NodeCacheService nodeCacheService,
            		       ApAccessPointRepository apRepository,
            		       FundVersionRepository fundVersionRepository,
            		       ResourcePathResolver resourcePathResolver,
                           final DataService dataService,
                           final ArrangementService arrangementService,
                           final StaticDataService staticDataService,
                           final NodeRepository nodeRepository,
                           final ItemRepository itemRepository,
                           final ApStateRepository stateRepository,
                           final ApItemRepository apItemRepository,
                           final ApBindingStateRepository bindingStateRepository,
                           final DataStringRepository dataStringRepository,
                           final ApIndexRepository indexRepository,
                           final InstitutionRepository institutionRepository,
                           final ScopeRepository scopeRepository,
                           final RuleService ruleService,
                           final ElzaLocale elzaLocale,
                           final AccessPointCacheService apcService) {
        this.initHelper = new ExportInitHelper(em, userService, levelRepository, nodeCacheService, apRepository,
                fundVersionRepository,
                resourcePathResolver,
                dataService, apcService);
        this.institutionRepository = institutionRepository;
        this.apItemRepository = apItemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.dataStringRepository = dataStringRepository;
        this.stateRepository = stateRepository;
        this.indexRepository = indexRepository;
        this.scopeRepository = scopeRepository;
        this.itemRepository = itemRepository;
        this.nodeRepository = nodeRepository;
        this.arrangementService = arrangementService;
        this.staticDataService = staticDataService;
        this.ruleService = ruleService;
        this.elzaLocale = elzaLocale;
    }

    public List<String> getTransformationNames() throws IOException {
        Path transformDir = initHelper.getResourcePathResolver().getExportTrasnformDir();
        if (!Files.exists(transformDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(transformDir);) {
            return files
                    .filter(p -> p.endsWith(".xslt"))
                    .map(p -> p.getFileName().toString())
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted().collect(Collectors.toList());
        }
    }

    /**
     * Exports data as XML to specified output stream.
     *
     * @param os
     *            generated XML
     * @param params
     *            export configuration
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    //TODO: Opravneni se musi hlidat dle typu exportovanych dat
    //@AuthMethod(permission = { UsrPermission.Permission.FUND_ADMIN })
    public void exportXmlData(OutputStream os, ExportBuilder builder, DEExportParams params) {
        exportData(os, builder, params);
    }

    private void exportData(OutputStream os, ExportBuilder builder, DEExportParams params) {
        log.debug("Exporting data, apIds={}, funds/sections={}", params.getApIds(), params.getFundsSections());

        // create export context
        ExportContext context = new ExportContext(builder, staticDataService.getData(), ObjectListIterator.getMaxBatchSize());
        context.setFundsSections(params.getFundsSections());
        if (params.getApIds() != null) {
            params.getApIds().forEach(context::addApId);
        }

        // prepare filter
        if (params.getExportFilter() != null) {
            RulExportFilter expFilterDB = ruleService.getExportFilter(params.getExportFilter());
            // create bean for export filter
            ExportFilterConfig efc = loadConfig(expFilterDB);
            ExportFilter expFilter = efc.createFilter(initHelper.getEm(), staticDataService.getData(), elzaLocale, initHelper.getDataService(),
            		new AccessPointCacheProvider(initHelper.getApCacheService()));
            context.setExportFilter(expFilter);
        }

        // set flags include AP && UUID
        context.setIncludeAccessPoints(params.isIncludeAccessPoints());
        context.setIncludeUUID(params.isIncludeUUID());

        // call all readers
        for (ExportPhase phase : ExportPhase.values()) {
            ExportReader reader = phase.createExportReader(context, initHelper);
            reader.read();
        }

        // write result
        try {
            log.debug("Building export file");

            builder.build(os);
        } catch (Exception e) {
            log.error("Failed to prepare export", e);
            throw new SystemException(e);
        } finally {
            log.debug("Cleaning export builder");
            try {
                builder.clear();
            } catch (Exception e) {
                log.error("Failed to clean export", e);
            }
        }
        log.debug("Export is done.");
    }

    /**
     * Check global and access point(s) permission
     *
     * @param params
     */
    private void checkGlobalAndAccessPointPermission(DEExportParams params) {
        UserDetail userDetail = initHelper.getUserService().getLoggedUserDetail();

        Collection<FundSections> sections = params.getFundsSections();

        // check global permission
        AuthorizationRequest authRequest = AuthorizationRequest
                .hasPermission(UsrPermission.Permission.FUND_ADMIN);
        if (!authRequest.matches(userDetail)) {
            // check section parts
            // check permissions for each exported part
            if (CollectionUtils.isNotEmpty(sections)) {
                for (FundSections fs : sections) {
                    checkExportPermission(fs, userDetail);
                }
            }
        }

        // check all access points
        if (CollectionUtils.isNotEmpty(sections) && params.isIncludeAccessPoints()) {

            Collection<Integer> fundVersionIds = sections.stream().map(s -> s.getFundVersionId()).collect(Collectors.toList());

            // find all arr_data_record_ref.record_id from arr_item from fund(s)
            List<Integer> recordIds = itemRepository.findArrDataRecordRefRecordIdsByFundVersionIds(fundVersionIds);
            Set<Integer> accessPointIds = new HashSet<>(recordIds);

            // find all children arr_data_record_ref.record_id from list of access point ids
            if (CollectionUtils.isNotEmpty(recordIds)) {
                ObjectListIterator.forEachPage(recordIds, page -> {
                    List<RefRecordsFromIds> results = apItemRepository.findArrDataRecordRefRecordIdsByAccessPointIds(page);
                    for (RefRecordsFromIds result : results) {
                        Integer recordId = result.getRecordId();
                        if (recordId == null) {
                            throw new BusinessException("Entita has unresolved reference(s)", BaseCode.INVALID_STATE)
                                .set("bindingId", result.getBindingId())
                                .set("accessPointId", result.getAccessPointId());
                        }
                        accessPointIds.add(recordId);
                    }
                });
            }

            // check all access points
            if (CollectionUtils.isNotEmpty(accessPointIds)) {
                ObjectListIterator.forEachPage(accessPointIds, page -> {
                	var validIdsCount = stateRepository.countValidByAccessPointIds(page);
                    if ( validIdsCount != page.size()) {
                    	log.error("Not all access points are valid, pageSize={}, validIdsCount={}, apIds={}.", page.size(), validIdsCount, page);
                        List<Integer> deletedApIds = stateRepository.findDeletedAccessPointIdsByAccessPointIds(page);
                        throw new BusinessException("Entity(-ies) has been deleted.", RegistryCode.CANT_EXPORT_DELETED_AP)
                                        .set(ApAccessPoint.FIELD_ACCESS_POINT_ID, deletedApIds);
                    }
                });
            }
        }
    }
    
    /**
     * Export fund 
     * 
     * @param params
     * @param xmlFile
     * @throws IOException
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    public void exportXmlDataToFile(DEExportParams params, Path xmlFile) throws IOException {

        checkGlobalAndAccessPointPermission(params);

        ExportBuilder exportBuilder = new XmlExportBuilder();

        // write response
        try (OutputStream os = Files.newOutputStream(xmlFile, StandardOpenOption.WRITE)) {
            exportData(os, exportBuilder, params);
        } catch(Exception e) {
            log.error("Failed to export data", e);
            throw e;
        }
    }

    /**
     * Export list of funds to csv
     * 
     * @param params
     * @param csvFile
     * @throws IOException
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    public void exportCsvDataToFile(SearchParams params, Path csvFile) throws IOException {
    	List<String[]> data = new ArrayList<>();

    	// for reading ruleSet by id
    	StaticDataProvider sdp = staticDataService.getData();

    	// get result
    	FindFundVersionsResult fundVersionsResult = arrangementService.findFundsBySearchParams(params);

    	ObjectListIterator.forEachPage(fundVersionsResult.getFundVersionList(), fundVersions -> {
    		List<Integer> fundIds = fundVersions.stream().map(fv -> fv.getFundId()).toList();
    		List<ParInstitution> institutions = institutionRepository.findAllByFundIds(fundIds);
    		List<Integer> rootNodeIds = fundVersions.stream().map(fv -> fv.getRootNodeId()).toList();

    		Map<Integer, ParInstitution> institutionMap = institutions.stream().collect(Collectors.toMap(ParInstitution::getInstitutionId, Function.identity()));
    		List<Integer> accessPointIds = institutions.stream().map(i -> i.getAccessPointId()).toList();
    		List<ApIndex> indexes = indexRepository.findPreferredPartIndexByAccessPointIdsAndIndexType(accessPointIds, DISPLAY_NAME);
    		Map<Integer, ApIndex> partIdIndexMap = indexes.stream().collect(Collectors.toMap(ApIndex::getPartId, Function.identity()));
    		List<ArrNode> rootNodes = nodeRepository.findAllById(rootNodeIds);
    		Map<Integer, String> nodeIdUuidMap = rootNodes.stream().collect(Collectors.toMap(ArrNode::getNodeId, ArrNode::getUuid));

    		fundVersions.forEach(fv -> {
	    		ArrFund fund = fv.getFund();
	    		ParInstitution parInstitution = institutionMap.get(fund.getInstitutionId());
	    		ApAccessPoint accessPoint = parInstitution.getAccessPoint();
	    		ApIndex index = partIdIndexMap.get(accessPoint.getPreferredPartId());
	    		RuleSet ruleSet = sdp.getRuleSetById(fv.getRuleSetId());

	    		String fundId = fv.getFundId().toString();
	    		String name = fund.getName();
	    		String createDate = fund.getCreateDate().toString();
	    		String internalCode = fund.getInternalCode();
	    		String fundNumber = fund.getFundNumber() == null ? null : fund.getFundNumber().toString();
	    		String unitDate = fund.getUnitdate();
	    		String mark = fund.getMark();
	    		String managed = Boolean.toString(fund.getManaged());
	    		String institutionId = fund.getInstitutionId().toString();
	    		String institutionCode = parInstitution.getInternalCode();
	    		String institutionName = index.getIndexValue();
	    		String fundversionId = fv.getFundVersionId().toString();
	    		String rootNodeId = fv.getRootNodeId().toString();
	    		String uuid = nodeIdUuidMap.get(fv.getRootNodeId());
	    		String rulesetCode = ruleSet.getCode();

	    		data.add(new String[]{
	    				fundId, name, createDate, internalCode, fundNumber, unitDate, mark, managed, 
	    				institutionId, institutionCode, institutionName,
	    				fundversionId, rootNodeId, uuid, rulesetCode
	    		});
    		});
    	});

    	String[] headers = {
    			"fundId", "name", "createDate", "internalCode", "fundNumber", "unitDate", "mark", "managed", 
				"institutionId", "institutionCode", "institutionName",
				"fundversionId", "rootNodeId", "uuid", "rulesetCode"    			
    	};

        // write result
        try (OutputStream os = Files.newOutputStream(csvFile, StandardOpenOption.WRITE);
             OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

        	csvPrinter.printRecord((Object[]) headers);
        	csvPrinter.printRecords(data);

        	csvPrinter.flush();
        } catch(Exception e) {
            log.error("Failed to export data", e);
            throw e;
        }
    }

    private static final int ACCESS_POINT_EXPORT_BATCH_SIZE = 500;

    private static final List<String> ACCESS_POINT_EXPORT_SECTION_PART_TYPE_CODES = List.of("PT_CRE", "PT_EXT", "PT_BODY");

    /**
     * Stream-export access points matching the given Lucene query to CSV.
     *
     * The implementation pages through the search to assemble all matching ids, then iterates them
     * sorted ascending in fixed-size batches. Each batch loads exactly the projection rows needed
     * for one CSV row group, writes them to the printer, and clears the persistence context so the
     * heap stays bounded even for very large result sets (hundreds of thousands of entities).
     *
     * The CSV is UTF-8 with a BOM so Czech diacritics open correctly in Excel.
     *
     * @param progressSink receives a 0..100 percentage after each batch; may not be {@code null}
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    public void exportAccessPointsCsv(SearchFilterVO searchFilter,
                                      Collection<Integer> apTypeIds,
                                      Collection<Integer> scopeIds,
                                      ApState.StateApproval state,
                                      RevStateApproval revState,
                                      Path csvFile,
                                      IntConsumer progressSink) throws IOException {
        StaticDataProvider sdp = staticDataService.getData();
        AccessPointCacheService apcService = initHelper.getApCacheService();
        ApAccessPointRepository apRepository = initHelper.getApRepository();
        EntityManager em = initHelper.getEm();

        List<Integer> allIds = apcService.searchAllIds(searchFilter, apTypeIds, scopeIds,
                                                      state, revState, sdp,
                                                      AccessPointCacheService.DEFAULT_SEARCH_ALL_PAGE_SIZE);
        final int total = allIds.size();

        ItemType nmMainType = sdp.getItemTypeByCode("NM_MAIN");
        ItemType nmMinorType = sdp.getItemTypeByCode("NM_MINOR");
        Integer nmMainItemTypeId = nmMainType != null ? nmMainType.getEntity().getItemTypeId() : null;
        Integer nmMinorItemTypeId = nmMinorType != null ? nmMinorType.getEntity().getItemTypeId() : null;
        List<RulItemType> nameItemTypes = new ArrayList<>(2);
        if (nmMainType != null) {
            nameItemTypes.add(nmMainType.getEntity());
        }
        if (nmMinorType != null) {
            nameItemTypes.add(nmMinorType.getEntity());
        }

        try (OutputStream os = Files.newOutputStream(csvFile, StandardOpenOption.WRITE)) {
            // UTF-8 BOM for Excel compatibility (Czech diacritics)
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);

            try (OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

                csvPrinter.printRecord(
                        "accessPointId", "uuid", "externalId",
                        "scope", "apType", "state",
                        "prefDisplayName", "nmMain", "nmMinor",
                        "ptCre", "ptExt", "ptBody");

                if (total == 0) {
                    csvPrinter.flush();
                    progressSink.accept(100);
                    return;
                }

                int processed = 0;
                for (int from = 0; from < total; from += ACCESS_POINT_EXPORT_BATCH_SIZE) {
                    int to = Math.min(from + ACCESS_POINT_EXPORT_BATCH_SIZE, total);
                    List<Integer> batch = allIds.subList(from, to);

                    writeAccessPointBatch(csvPrinter, batch, apRepository,
                                          nameItemTypes, nmMainItemTypeId, nmMinorItemTypeId);

                    csvPrinter.flush();
                    em.clear();

                    processed += batch.size();
                    progressSink.accept((int) ((long) processed * 100L / total));
                }
            }
        } catch (Exception e) {
            log.error("Failed to export access points", e);
            throw e;
        }
    }

    private void writeAccessPointBatch(CSVPrinter out,
                                       List<Integer> ids,
                                       ApAccessPointRepository apRepository,
                                       List<RulItemType> nameItemTypes,
                                       Integer nmMainItemTypeId,
                                       Integer nmMinorItemTypeId) throws IOException {
        List<ApAccessPoint> aps = apRepository.findAllById(ids);
        Map<Integer, ApAccessPoint> apMap = aps.stream()
                .collect(Collectors.toMap(ApAccessPoint::getAccessPointId, Function.identity()));

        List<ApState> states = stateRepository.findLastByAccessPointIdsFetchScopeAndApType(ids);
        Map<Integer, ApState> stateMap = states.stream()
                .collect(Collectors.toMap(ApState::getAccessPointId, Function.identity(), (a, b) -> a));

        List<ApBindingState> bindings = bindingStateRepository.findActiveByAccessPointIdIn(ids);
        Map<Integer, String> externalIdMap = bindings.stream()
                .collect(Collectors.toMap(ApBindingState::getAccessPointId,
                                          b -> b.getBinding().getValue(),
                                          (a, b) -> a));

        List<ApIndex> prefIndexes = indexRepository
                .findPreferredPartIndexByAccessPointIdsAndIndexType(ids, DISPLAY_NAME);
        Map<Integer, String> prefDisplayMap = prefIndexes.stream()
                .collect(Collectors.toMap(idx -> idx.getPart().getAccessPointId(),
                                          ApIndex::getIndexValue,
                                          (a, b) -> a));

        List<ApIndex> sectionIndexes = indexRepository
                .findIndexByAccessPointIdsAndPartTypeCodesAndIndexType(ids,
                        ACCESS_POINT_EXPORT_SECTION_PART_TYPE_CODES, DISPLAY_NAME);
        Map<Integer, Map<String, String>> sectionMap = new HashMap<>();
        for (ApIndex idx : sectionIndexes) {
            int apId = idx.getPart().getAccessPointId();
            String code = idx.getPart().getPartType().getCode();
            sectionMap.computeIfAbsent(apId, k -> new HashMap<>()).put(code, idx.getIndexValue());
        }

        Map<Integer, Map<Integer, String>> nameMap = new HashMap<>();
        if (!nameItemTypes.isEmpty()) {
            List<PreferredPartNameItem> items = apItemRepository
                    .findPreferredPartNameItemsByAccessPointIdsAndItemTypes(ids, nameItemTypes);
            if (!items.isEmpty()) {
                List<Integer> dataIds = items.stream()
                        .map(PreferredPartNameItem::getDataId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Map<Integer, String> dataValueMap;
                if (dataIds.isEmpty()) {
                    dataValueMap = Collections.emptyMap();
                } else {
                    dataValueMap = dataStringRepository.findValuesByDataIdIn(dataIds).stream()
                            .collect(Collectors.toMap(DataStringRepository.OnlyValues::getDataId,
                                                      DataStringRepository.OnlyValues::getStringValue,
                                                      (a, b) -> a));
                }
                for (PreferredPartNameItem item : items) {
                    String value = item.getDataId() != null ? dataValueMap.get(item.getDataId()) : null;
                    nameMap.computeIfAbsent(item.getAccessPointId(), k -> new HashMap<>())
                           .put(item.getItemTypeId(), value);
                }
            }
        }

        for (Integer apId : ids) {
            ApAccessPoint ap = apMap.get(apId);
            ApState st = stateMap.get(apId);
            Map<String, String> sections = sectionMap.getOrDefault(apId, Collections.emptyMap());
            Map<Integer, String> names = nameMap.getOrDefault(apId, Collections.emptyMap());

            String uuid = ap != null && ap.getUuid() != null ? ap.getUuid() : "";
            String scopeCode = (st != null && st.getScope() != null) ? st.getScope().getCode() : "";
            String apTypeCode = (st != null && st.getApType() != null) ? st.getApType().getCode() : "";
            String stateName = (st != null && st.getStateApproval() != null) ? st.getStateApproval().name() : "";

            out.printRecord(
                    apId,
                    uuid,
                    externalIdMap.getOrDefault(apId, ""),
                    scopeCode,
                    apTypeCode,
                    stateName,
                    prefDisplayMap.getOrDefault(apId, ""),
                    nmMainItemTypeId != null ? nullToEmpty(names.get(nmMainItemTypeId)) : "",
                    nmMinorItemTypeId != null ? nullToEmpty(names.get(nmMinorItemTypeId)) : "",
                    sections.getOrDefault("PT_CRE", ""),
                    sections.getOrDefault("PT_EXT", ""),
                    sections.getOrDefault("PT_BODY", "")
            );
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Check permissions to export give FundSections
     *
     * @param fs
     *            fund section
     * @param userDetail
     */
    private void checkExportPermission(FundSections fs, UserDetail userDetail) {
        int versionId = fs.getFundVersionId();
        ArrFundVersion fundVersion = initHelper.getFundVersionRepository().getOneCheckExist(versionId);

        AuthorizationRequest exportAuthReq = AuthorizationRequest
                .hasPermission(UsrPermission.Permission.FUND_EXPORT_ALL)
                .or(UsrPermission.Permission.FUND_EXPORT, fundVersion);
        if (!exportAuthReq.matches(userDetail)) {
            // throw exception - authorization not granted
            UsrPermission.Permission deniedPermissions[] = { UsrPermission.Permission.FUND_EXPORT_ALL,
                    UsrPermission.Permission.FUND_EXPORT };
            throw new AccessDeniedException("Missing permissions: " + Arrays.toString(deniedPermissions),
                    deniedPermissions);
        }

        Set<Integer> scopeIds = this.scopeRepository.findIdsByFundId(fundVersion.getFundVersionId());
        scopeIds.forEach(scopeId -> {
            // test permissions for scope id
            AuthorizationRequest authReq = AuthorizationRequest
                    .hasPermission(UsrPermission.Permission.AP_SCOPE_RD_ALL)
                    .or(UsrPermission.Permission.AP_SCOPE_RD, scopeId);
            if (!authReq.matches(userDetail)) {
                // throw exception - authorization not granted
                UsrPermission.Permission deniedPermissions[] = { UsrPermission.Permission.AP_SCOPE_RD_ALL,
                        UsrPermission.Permission.AP_SCOPE_RD };
                throw new AccessDeniedException(
                        "Missing permissions: " + Arrays.toString(deniedPermissions),
                        deniedPermissions);
            }
        });
    }

    /**
     * Read config from export filter file .yaml
     *
     * @param expFilterDB file name
     * @return ExportFilterConfig
     */
    private ExportFilterConfig loadConfig(RulExportFilter expFilterDB) {
        ResourcePathResolver resourcePathResolver = initHelper.getResourcePathResolver();
        Path rulesetExportFilter = resourcePathResolver.getExportFilterFile(expFilterDB);

        // register type descriptors
        Constructor yamlCtor = new Constructor(new LoaderOptions());
        yamlCtor.addTypeDescription(new TypeDescription(AccessRestrictConfig.class, "!ExportFilterConfig"));
        yamlCtor.addTypeDescription(new TypeDescription(EntityProperties.class, "!EntityProperties"));
        yamlCtor.addTypeDescription(new TypeDescription(And.class, "!And"));
        yamlCtor.addTypeDescription(new TypeDescription(Not.class, "!Not"));
        yamlCtor.addTypeDescription(new TypeDescription(PartCondition.class, "!Part"));
        Yaml yamlLoader = new Yaml(yamlCtor);

        ExportFilterConfig efc;
        try (InputStream inputStream = new FileInputStream(rulesetExportFilter.toFile())) {
            efc = yamlLoader.load(inputStream);
        } catch (IOException e) {
            log.error("Failed to read yaml file {}", rulesetExportFilter, e);
            throw new SystemException(e);
        }

        return efc;
    }
}
