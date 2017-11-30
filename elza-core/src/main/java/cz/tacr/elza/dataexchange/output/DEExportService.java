package cz.tacr.elza.dataexchange.output;

import java.io.OutputStream;
import java.util.List;

import javax.persistence.EntityManager;
import javax.xml.stream.XMLStreamException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.core.data.StaticDataService;
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
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.utils.XmlUtils;

/**
 * Service for data-exchange export.
 */
@Service
public class DEExportService {

    private final ExportInitHelper initHelper;

    private final StaticDataService staticDataService;

    private final String transformationsDir;

    @Autowired
    public DEExportService(EntityManager em,
                           StaticDataService staticDataService,
                           FundVersionRepository fundVersionRepository,
                           UserService userService,
                           LevelRepository levelRepository,
                           NodeCacheService nodeCacheService,
                           RegRecordRepository recordRepository,
                           @Value("${elza.xmlExport.transformationDir}") String transformationsDir) {
        this.initHelper = new ExportInitHelper(em, userService, levelRepository, nodeCacheService, recordRepository,
                fundVersionRepository);
        this.staticDataService = staticDataService;
        this.transformationsDir = transformationsDir;
    }

    public List<String> getTransformationNames() {
        return XmlUtils.getXsltFileNames(transformationsDir);
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
