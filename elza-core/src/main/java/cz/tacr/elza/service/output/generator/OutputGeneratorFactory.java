package cz.tacr.elza.service.output.generator;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.InstitutionRepository;
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

    private final InstitutionRepository institutionRepository;
    
    private final ApDescriptionRepository apDescRepository;
    
    private final ApNameRepository apNameRepository;
    
    private final ApExternalIdRepository apEidRepository;

    private final ApplicationContext applicationContext;

    private ElzaLocale elzaLocale;

    @Autowired
    public OutputGeneratorFactory(ApplicationContext applicationContext,
            StaticDataService staticDataService,
                                  ElzaLocale elzaLocale,
            FundTreeProvider fundTreeProvider,
            NodeCacheService nodeCacheService,
            InstitutionRepository institutionRepository,
            ApDescriptionRepository apDescRepository,
            ApNameRepository apNameRepository,
            ApExternalIdRepository apEidRepository,
            EntityManager em,
            DmsService dmsService,
            DEExportService exportService) {
        this.applicationContext = applicationContext;
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.institutionRepository = institutionRepository;
        this.apDescRepository = apDescRepository;
        this.apNameRepository = apNameRepository;
        this.apEidRepository = apEidRepository;
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
        case DE_XML:
            return createDEXmlOutputGenerator();
        default:
            throw new IllegalStateException("Uknown output engine, name:" + engine);
        }
    }

    public FreemarkerOutputGenerator createFreemarkerOutputGenerator() {
        return new FreemarkerOutputGenerator(staticDataService, elzaLocale, fundTreeProvider, nodeCacheService,
                institutionRepository, apDescRepository, apNameRepository, apEidRepository, em, dmsService);
    }

    public JasperOutputGenerator createJasperOutputGenerator() {
        return new JasperOutputGenerator(applicationContext, staticDataService, elzaLocale,
                fundTreeProvider, nodeCacheService,
                institutionRepository,
                apDescRepository, apNameRepository, apEidRepository, em, dmsService);
    }

    public DEXmlOutputGenerator createDEXmlOutputGenerator() {
        return new DEXmlOutputGenerator(em, dmsService, exportService);
    }
}
