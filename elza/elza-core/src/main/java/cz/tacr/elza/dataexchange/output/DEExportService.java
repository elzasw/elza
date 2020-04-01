package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.net.HttpHeaders;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.DEExportController.DEExportParamsVO;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.db.HibernateConfiguration;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportPhase;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Service for data-exchange export.
 */
@Service
public class DEExportService {

    private final ExportInitHelper initHelper;

    private final StaticDataService staticDataService;

    private final ScopeRepository scopeRepository;

    @Autowired
    public DEExportService(EntityManager em,
            StaticDataService staticDataService,
            FundVersionRepository fundVersionRepository,
            UserService userService,
            LevelRepository levelRepository,
            NodeCacheService nodeCacheService,
            ApAccessPointRepository apRepository,
            ResourcePathResolver resourcePathResolver,
            ScopeRepository scopeRepository) {
        this.initHelper = new ExportInitHelper(em, userService, levelRepository, nodeCacheService, apRepository,
                fundVersionRepository,
                resourcePathResolver);
        this.staticDataService = staticDataService;
        this.scopeRepository = scopeRepository;
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
        // create export context
        ExportContext context = new ExportContext(builder, staticDataService.getData(),
                HibernateConfiguration.MAX_IN_SIZE);
        context.setFundsSections(params.getFundsSections());
        if (params.getApIds() != null) {
            params.getApIds().forEach(context::addApId);
        }
        if (params.getPartyIds() != null) {
            params.getPartyIds().forEach(context::addPartyId);
        }

        // call all readers
        for (ExportPhase phase : ExportPhase.values()) {
            ExportReader reader = phase.createExportReader(context, initHelper);
            reader.read();
        }

        // write result
        try {
            builder.build(os);
        } catch (XMLStreamException e) {
            throw new SystemException(e);
        } finally {
            builder.clear();
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
}
