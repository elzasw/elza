package cz.tacr.elza.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;

/**
 * Main administration service
 *
 * @since 19. 1. 2016
 */
@Component
public class AdminService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    private MassIndexingMonitor massIndexingMonitor;

    private Future<?> indexerStatus;

    /** Přeindexuje všechna data. */
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public void reindex() {
        reindexInternal();
    }

    /**
     * Volání reindexu bez kontroly práv
     *
     * Volání s časovačem, ve výchozím stavu: 0 0 4 ? * SAT
     * co znamená: každou sobotu ve 04:00
     * @throws InterruptedException 
     */
    @Scheduled(cron = "${elza.reindex.cron:0 0 4 ? * SAT}")
    public void reindexInternal() {
    	if (isIndexingRunning()) {
    		return;
    	}

    	SearchSession searchSession = Search.session(entityManager);
    	MassIndexer massIndexer = searchSession.massIndexer(ArrCachedNode.class, ArrDescItem.class); // TODO add ArrDescItem.class
    	//massIndexer.monitor(massIndexingMonitor);
    	indexerStatus = massIndexer.start().toCompletableFuture();
    }

    /**
     * Zjistí zda běží indexování.
     *
     * @return true pokud běží indexování, jinak false
     */
    @ResponseBody
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public boolean isIndexingRunning() {
        if (indexerStatus != null) {
            return !indexerStatus.isDone();
        }

        return false;
    }

    @AuthMethod(permission = { UsrPermission.Permission.ADMIN, UsrPermission.Permission.FUND_ARR_ALL,
            UsrPermission.Permission.FUND_ARR })
    public List<TreeNodeVO> findNodeByIds(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                          final List<Integer> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyList();
        }
        return levelTreeCacheService.getNodesByIds(nodeIds, fundVersion);
    }
}
