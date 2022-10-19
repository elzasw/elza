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
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.NodeCacheService;

@Service
public class OutputGeneratorFactory {

    private final StaticDataService staticDataService;

    private final FundRepository fundRepository;

    private final FundTreeProvider fundTreeProvider;

    private final NodeCacheService nodeCacheService;

    private final EntityManager em;

    private final DmsService dmsService;

    private final DEExportService exportService;

    private final InstitutionRepository institutionRepository;

    private final ApStateRepository apStateRepository;

    private final ApBindingRepository bindingRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApItemRepository itemRepository;

    private final DaoLinkRepository daoLinkRepository;

    private final ApplicationContext applicationContext;

    private final ApIndexRepository indexRepository;
    
    private final AccessPointCacheService accessPointCacheService;

    private ElzaLocale elzaLocale;

    @Autowired
    public OutputGeneratorFactory(final ApplicationContext applicationContext,
                                  final StaticDataService staticDataService,
                                  final ElzaLocale elzaLocale,
                                  final FundRepository fundRepository,
                                  final FundTreeProvider fundTreeProvider,
                                  final NodeCacheService nodeCacheService,
                                  final InstitutionRepository institutionRepository,
                                  final ApStateRepository apStateRepository,
                                  final ApBindingRepository bindingRepository,
                                  final ApItemRepository itemRepository,
                                  final ApBindingStateRepository bindingStateRepository,
                                  final ApIndexRepository indexRepository,
                                  final EntityManager em,
                                  final DmsService dmsService,
                                  final DEExportService exportService,
                                  final DaoLinkRepository daoLinkRepository,
                                  final AccessPointCacheService accessPointCacheService) {
        this.applicationContext = applicationContext;
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
        this.fundRepository = fundRepository;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.institutionRepository = institutionRepository;
        this.apStateRepository = apStateRepository;
        this.bindingRepository = bindingRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.indexRepository = indexRepository;
        this.em = em;
        this.dmsService = dmsService;
        this.exportService = exportService;
        this.daoLinkRepository = daoLinkRepository;
        this.accessPointCacheService = accessPointCacheService;
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
        return new FreemarkerOutputGenerator(applicationContext, staticDataService, elzaLocale,
                fundRepository, fundTreeProvider,
                nodeCacheService,
                institutionRepository, apStateRepository, bindingRepository, itemRepository,
                bindingStateRepository, indexRepository, em, dmsService,
                daoLinkRepository, accessPointCacheService);
    }

    public JasperOutputGenerator createJasperOutputGenerator() {
        return new JasperOutputGenerator(applicationContext, staticDataService, elzaLocale,
                fundRepository, fundTreeProvider,
                nodeCacheService,
                institutionRepository, apStateRepository,
                bindingRepository, itemRepository, bindingStateRepository,
                indexRepository, em, dmsService, daoLinkRepository, accessPointCacheService);
    }

    public DEXmlOutputGenerator createDEXmlOutputGenerator() {
        return new DEXmlOutputGenerator(em, dmsService, exportService);
    }
}
