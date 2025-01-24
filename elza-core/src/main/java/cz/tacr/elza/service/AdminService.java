package cz.tacr.elza.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApCachedAccessPoint;
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
	
	private static final Logger log = LoggerFactory.getLogger(AdminService.class);

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
     * @return 
     * @throws InterruptedException 
     */    
    public Future<?> reindexInternal() {
    	if (isIndexingRunning()) {
    		log.debug("Reindexing already running.");
    		return indexerStatus;
    	}
    	log.info("Started mass indexing ...");

    	SearchSession searchSession = Search.session(entityManager);
    	MassIndexer massIndexer = searchSession.massIndexer(ArrCachedNode.class, ArrDescItem.class, ApCachedAccessPoint.class);
    	CompletionStage<?> startResult = massIndexer.start();
    	indexerStatus = startResult.whenComplete((r, ex) -> {
    		if(ex!=null) {
    			log.error("Indexing failed.", ex);
    		} else {
    			log.info("Mass indexing finished.");
    		}
    	}).toCompletableFuture();
    	
    	return indexerStatus;
    }

    /**
     * Method to run reindex at specific time.
     * 
     * Reindex might be disabled setting special value - in the cron expression
     */
    @Scheduled(cron = "${elza.reindex.cron:0 0 4 ? * SAT}")
    @Transactional
    public void reindexTimer() {
    	reindexInternal();
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
