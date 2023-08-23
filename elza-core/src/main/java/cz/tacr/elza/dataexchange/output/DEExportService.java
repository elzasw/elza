package cz.tacr.elza.dataexchange.output;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.common.net.HttpHeaders;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.controller.DEExportController.DEExportParamsVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportPhase;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.filters.AccessRestrictConfig;
import cz.tacr.elza.dataexchange.output.filters.ExportFilter;
import cz.tacr.elza.dataexchange.output.filters.ExportFilterConfig;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Service for data-exchange export.
 */
@Service
public class DEExportService {

    private final static Logger log = LoggerFactory.getLogger(DEExportService.class);

    private final ExportInitHelper initHelper;

    private final StaticDataService staticDataService;

    private final ApStateRepository stateRepository;

    private final ApItemRepository apItemRepository;

    private final ScopeRepository scopeRepository;

    private final ItemRepository itemRepository;

    private final RuleService ruleService;

    @Autowired
    public DEExportService(EntityManager em,
            StaticDataService staticDataService,
            FundVersionRepository fundVersionRepository,
            UserService userService,
            ItemRepository itemRepository,
            LevelRepository levelRepository,
            ApStateRepository stateRepository,
            ApItemRepository apItemRepository,
            NodeCacheService nodeCacheService,
            ApAccessPointRepository apRepository,
            ResourcePathResolver resourcePathResolver,
            ScopeRepository scopeRepository,
            final RuleService ruleService) {
        this.initHelper = new ExportInitHelper(em, userService, levelRepository, nodeCacheService, apRepository,
                fundVersionRepository,
                resourcePathResolver);
        this.staticDataService = staticDataService;
        this.apItemRepository = apItemRepository;
        this.stateRepository = stateRepository; 
        this.scopeRepository = scopeRepository;
        this.itemRepository = itemRepository;
        this.ruleService = ruleService;
    }

    public List<String> getTransformationNames() throws IOException {
        Path transformDir = initHelper.getResourcePathResolver().getExportXmlTrasnformDir();
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
        ExportContext context = new ExportContext(builder, staticDataService.getData(),
                                                  ObjectListIterator.getMaxBatchSize());
        context.setFundsSections(params.getFundsSections());
        if (params.getApIds() != null) {
            params.getApIds().forEach(context::addApId);
        }

        // prepare filter
        if (params.getExportFilterId() != null) {
            RulExportFilter expFilterDB = ruleService.getExportFilter(params.getExportFilterId());
            // create bean for export filter
            ExportFilterConfig efc = loadConfig(expFilterDB);
            ExportFilter expFilter = efc.createFilter(initHelper.getEm(), staticDataService.getData());
            context.setExportFilter(expFilter);
        }

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
        if (CollectionUtils.isNotEmpty(sections)) {

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
                    if (stateRepository.countValidByAccessPointIds(page) != page.size()) {
                        throw new BusinessException("Entity(es) has been deleted", BaseCode.INVALID_STATE)
                            .set("IDs", stateRepository.findDeletedAccessPointIdsByAccessPointIds(page));
                    }
                });
            }
        }
    }
    
    /**
     * Export fund 
     * 
     * @param params
     * @throws IOException
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    public void exportXmlDataToFile(DEExportParams request, Path xmlFile) throws IOException {

        checkGlobalAndAccessPointPermission(request);

        ExportBuilder exportBuilder = new XmlExportBuilder();

        // write response
        try (OutputStream os = Files.newOutputStream(xmlFile, StandardOpenOption.WRITE)) {
            exportData(os, exportBuilder, request);
        } catch(Exception e) {
            log.error("Failed to export data", e);
            throw e;
        }
    }

    /**
     * Export data
     * 
     * @param response
     * @param params
     * @throws IOException
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    public void exportXmlData(HttpServletResponse response, DEExportParamsVO params) throws IOException {

        checkGlobalAndAccessPointPermission(params);

        // file headers
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        FileDownload.addContentDispositionAsAttachment(response, "elza-data.xml");

        // cache headers
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setDateHeader(HttpHeaders.EXPIRES, 0);

        ExportBuilder exportBuilder = new XmlExportBuilder();

        // write response
        try (ServletOutputStream os = response.getOutputStream()) {
            response.flushBuffer();
            exportData(os, exportBuilder, params);
            response.flushBuffer();
        } catch(Exception e) {
        	log.error("Failed to export data", e);
        	throw e;
        }
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
        Constructor yamlCtor = new Constructor();
        yamlCtor.addTypeDescription(new TypeDescription(AccessRestrictConfig.class, "!ExportFilterConfig"));
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
