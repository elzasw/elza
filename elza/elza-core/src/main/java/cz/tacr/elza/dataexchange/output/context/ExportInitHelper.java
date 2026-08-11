package cz.tacr.elza.dataexchange.output.context;

import jakarta.persistence.EntityManager;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cache.NodeCacheService;

public class ExportInitHelper {

    private final EntityManager em;

    private final UserService userService;

    private final LevelRepository levelRepository;

    private final NodeCacheService nodeCacheService;

    private final ApAccessPointRepository apRepository;

    private final ArrDaLinkRepository daLinkRepository;

    private final FundVersionRepository fundVersionRepository;

    private final ResourcePathResolver resourcePathResolver;
    
    private final DataService dataService;
    
    private final AccessPointCacheService apcService;

    public ExportInitHelper(final EntityManager em,
                            final UserService userService,
                            final LevelRepository levelRepository,
                            final NodeCacheService nodeCacheService,
                            final ApAccessPointRepository apRepository,
                            final ArrDaLinkRepository daLinkRepository,
                            final FundVersionRepository fundVersionRepository,
                            final ResourcePathResolver resourcePathResolver,
                            final DataService dataService,
                            final AccessPointCacheService apcService) {
        this.em = em;
        this.userService = userService;
        this.levelRepository = levelRepository;
        this.nodeCacheService = nodeCacheService;
        this.apRepository = apRepository;
        this.daLinkRepository = daLinkRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.resourcePathResolver = resourcePathResolver;
        this.dataService = dataService;
        this.apcService = apcService;
    }

	public EntityManager getEm() {
        return em;
    }
	
	public AccessPointCacheService getApCacheService() {
		return apcService;
	}

    public UserService getUserService() {
        return userService;
    }

    public LevelRepository getLevelRepository() {
        return levelRepository;
    }

    public NodeCacheService getNodeCacheService() {
        return nodeCacheService;
    }

    public ApAccessPointRepository getApRepository() {
        return apRepository;
    }

    public ArrDaLinkRepository getDaLinkRepository() {
        return daLinkRepository;
    }

    public FundVersionRepository getFundVersionRepository() {
        return fundVersionRepository;
    }

    public ResourcePathResolver getResourcePathResolver() {
        return resourcePathResolver;
    }

    public DataService getDataService() {
		return dataService;
	}
}
