package cz.tacr.elza.service.output.generator;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.dataexchange.output.DEExportService;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.cache.NodeCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;

@Service
public class OutputGeneratorFactory {

    private final StaticDataService staticDataService;

    private final FundTreeProvider fundTreeProvider;

    private final NodeCacheService nodeCacheService;

    private final EntityManager em;

    private final DmsService dmsService;

    private final DEExportService exportService;

    private final InstitutionRepository institutionRepository;

    private final ApStateRepository apStateRepository;

    private final ApBindingRepository bindingRepository;

    private final ApPartRepository partRepository;

    private final ApItemRepository itemRepository;

    private final ApplicationContext applicationContext;

    private ElzaLocale elzaLocale;

    @Autowired
    public OutputGeneratorFactory(ApplicationContext applicationContext,
            StaticDataService staticDataService,
            ElzaLocale elzaLocale,
            FundTreeProvider fundTreeProvider,
            NodeCacheService nodeCacheService,
            InstitutionRepository institutionRepository,
            ApStateRepository apStateRepository,
            ApBindingRepository bindingRepository,
            ApPartRepository partRepository,
            ApItemRepository itemRepository,
            EntityManager em,
            DmsService dmsService,
            DEExportService exportService) {
        this.applicationContext = applicationContext;
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.institutionRepository = institutionRepository;
        this.apStateRepository = apStateRepository;
        this.bindingRepository = bindingRepository;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
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
        return new FreemarkerOutputGenerator(applicationContext, staticDataService, elzaLocale, fundTreeProvider,
                nodeCacheService,
                institutionRepository, apStateRepository, bindingRepository, partRepository, itemRepository, em, dmsService);
    }

    public JasperOutputGenerator createJasperOutputGenerator() {
        return new JasperOutputGenerator(applicationContext, staticDataService, elzaLocale,
                fundTreeProvider, nodeCacheService,
                institutionRepository, apStateRepository,
                bindingRepository, partRepository, itemRepository, em, dmsService);
    }

    public DEXmlOutputGenerator createDEXmlOutputGenerator() {
        return new DEXmlOutputGenerator(em, dmsService, exportService);
    }
}
