package cz.tacr.elza.dataexchange.output.context;

import javax.persistence.EntityManager;

import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;

public class ExportInitHelper {

    private final EntityManager em;

    private final UserService userService;

    private final LevelRepository levelRepository;

    private final NodeCacheService nodeCacheService;

    private final ApAccessPointRepository accessPointRepository;

    private final FundVersionRepository fundVersionRepository;

    private final AccessPointDataService accessPointDataService;

    public ExportInitHelper(EntityManager em,
                            UserService userService,
                            LevelRepository levelRepository,
                            NodeCacheService nodeCacheService,
                            ApAccessPointRepository accessPointRepository,
                            FundVersionRepository fundVersionRepository, AccessPointDataService accessPointDataService) {
        this.em = em;
        this.userService = userService;
        this.levelRepository = levelRepository;
        this.nodeCacheService = nodeCacheService;
        this.accessPointRepository = accessPointRepository;
        this.fundVersionRepository = fundVersionRepository;
        this.accessPointDataService = accessPointDataService;
    }

    public EntityManager getEntityManager() {
        return em;
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

    public ApAccessPointRepository getAccessPointRepository() {
        return accessPointRepository;
    }

    public FundVersionRepository getFundVersionRepository() {
        return fundVersionRepository;
    }

    public AccessPointDataService getAccessPointDataService() {
        return accessPointDataService;
    }
}
