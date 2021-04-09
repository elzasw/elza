package cz.tacr.elza.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.search.IndexerProgressMonitor;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 1. 2016
 */
@Component
public class AdminService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private IndexerProgressMonitor indexerProgressMonitor;

    @Autowired
    private ArrangementService arrangementService;

    private Future<?> indexerStatus;

    /** Přeindexuje všechna data. */
    @AuthMethod(permission = {UsrPermission.Permission.ADMIN})
    public void reindex() {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

        if (isIndexingRunning()) {
            return;
        }

        MassIndexer createIndexer = fullTextEntityManager.createIndexer();
        createIndexer.progressMonitor(indexerProgressMonitor);
        indexerStatus = createIndexer.start();
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
