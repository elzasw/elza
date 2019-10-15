package cz.tacr.elza.dataexchange.output.context;

import javax.persistence.EntityManager;

import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;

public class ExportInitHelper {

    private final EntityManager em;

    private final UserService userService;

    private final LevelRepository levelRepository;

    private final NodeCacheService nodeCacheService;

    private final ApAccessPointRepository apRepository;

    private final FundVersionRepository fundVersionRepository;

    public ExportInitHelper(EntityManager em,
            UserService userService,
            LevelRepository levelRepository,
            NodeCacheService nodeCacheService,
            ApAccessPointRepository apRepository,
            FundVersionRepository fundVersionRepository) {
        this.em = em;
        this.userService = userService;
        this.levelRepository = levelRepository;
        this.nodeCacheService = nodeCacheService;
        this.apRepository = apRepository;
        this.fundVersionRepository = fundVersionRepository;
    }

    public EntityManager getEm() {
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

    public ApAccessPointRepository getApRepository() {
        return apRepository;
    }

    public FundVersionRepository getFundVersionRepository() {
        return fundVersionRepository;
    }
}
