package cz.tacr.elza.service.output.generator;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;

@Service
public class OutputGeneratorFactory {

    private final StaticDataService staticDataService;

    private final FundTreeProvider fundTreeProvider;

    private final NodeCacheService nodeCacheService;

    private final EntityManager em;

    private final DmsService dmsService;

    private final DEExportService exportService;

    @Autowired
    public OutputGeneratorFactory(StaticDataService staticDataService,
                                  FundTreeProvider fundTreeProvider,
                                  NodeCacheService nodeCacheService,
                                  EntityManager em,
                                  DmsService dmsService,
                                  DEExportService exportService) {
        this.staticDataService = staticDataService;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.em = em;
        this.dmsService = dmsService;
        this.exportService = exportService;
    }

    public OutputGenerator createOutputGenerator(Engine engine) {
        switch (engine) {
            case FREEMARKER:
                return createFreemarkerOutputGenerator();
            case JASPER:
                return createJasperOutputGenerator();
            case DEXML:
                return createDEXmlOutputGenerator();
            default:
                throw new IllegalStateException("Uknown output engine, name:" + engine);
        }
    }

    public FreemarkerOutputGenerator createFreemarkerOutputGenerator() {
        return new FreemarkerOutputGenerator(staticDataService, fundTreeProvider, nodeCacheService, em, dmsService);
    }

    public JasperOutputGenerator createJasperOutputGenerator() {
        return new JasperOutputGenerator(staticDataService, fundTreeProvider, nodeCacheService, em, dmsService);
    }

    public DEXmlOutputGenerator createDEXmlOutputGenerator() {
        return new DEXmlOutputGenerator(em, dmsService, exportService);
    }
}
