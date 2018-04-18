package cz.tacr.elza.dataexchange.output;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;

import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.service.AccessPointDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportPhase;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.xml.XmlExportBuilder;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Service for data-exchange export.
 */
@Service
public class DEExportService {

    private final ExportInitHelper initHelper;

    private final StaticDataService staticDataService;

    private final ResourcePathResolver resourcePathResolver;

    @Autowired
    public DEExportService(EntityManager em,
                           StaticDataService staticDataService,
                           FundVersionRepository fundVersionRepository,
                           UserService userService,
                           LevelRepository levelRepository,
                           NodeCacheService nodeCacheService,
                           ApAccessPointRepository accessPointRepository,
                           ResourcePathResolver resourcePathResolver,
                           AccessPointDataService accessPointDataService) {
        this.initHelper = new ExportInitHelper(em, userService, levelRepository, nodeCacheService, accessPointRepository,
                fundVersionRepository, accessPointDataService);
        this.staticDataService = staticDataService;
        this.resourcePathResolver = resourcePathResolver;
    }

    public List<String> getTransformationNames() throws IOException {
        Path transformDir = resourcePathResolver.getExportXmlTrasnformDir();
        if (!Files.exists(transformDir)) {
            return Collections.emptyList();
        }
        return Files.list(transformDir)
                .filter(p -> p.endsWith(".xslt"))
                .map(p -> p.getFileName().toString())
                .map(n -> n.substring(0, n.length() - 5))
                .sorted().collect(Collectors.toList());
    }

    /**
     * Exports data as XML to specified output stream.
     *
     * @param os generated XML
     * @param params export configuration
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true)
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ADMIN })
    public void exportXmlData(OutputStream os, DEExportParams params) {
        exportData(os, new XmlExportBuilder(), params);
    }

    private void exportData(OutputStream os, ExportBuilder builder, DEExportParams params) {
        // create export context
        ExportContext context = new ExportContext(builder, staticDataService.getData(), 1000);
        context.setFundsSections(params.getFundsSections());
        if (params.getApIds() != null) {
            params.getApIds().forEach(context::addAPId);
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
}
