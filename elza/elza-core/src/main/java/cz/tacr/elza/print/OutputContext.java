package cz.tacr.elza.print;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Class for OutputContext
 * 
 * Allow access to the services
 */
@Component
@Scope("prototype")
public class OutputContext {

    @Autowired
    private GroovyService groovyService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private SchemaManager schemaManager;

    @Autowired
    private AccessPointDataService apDataService;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private EntityManager em;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private ApAccessPointRepository apRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    private ExportInitHelper exportInitHelper;

    public OutputContext() {
    }

    public StaticDataProvider getStaticData() {
        return staticDataService.getData();
    }

    public GroovyService getGroovyService() {
        return groovyService;
    }

    public SchemaManager getSchemaManager() {
        return schemaManager;
    }

    public AccessPointDataService getApDataService() {
        return apDataService;
    }

    public CachedAccessPoint findCachedAccessPoint(Integer accessPointId) {
        return accessPointCacheService.findCachedAccessPoint(accessPointId);
    }

    public ExportInitHelper getExportInitHelper() {
        if (exportInitHelper == null) {
            exportInitHelper = new ExportInitHelper(em, userService, levelRepository, nodeCacheService,
                    apRepository,
                    fundVersionRepository,
                    resourcePathResolver);
        }
        return exportInitHelper;
    }
}
