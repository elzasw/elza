package cz.tacr.elza.dataexchange.output.sections;

import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.security.Authorization;
import cz.tacr.elza.dataexchange.output.DEExportException;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundSections;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Reads levels and packets for funds sections specified by context.
 */
public class SectionsReader implements ExportReader {

    private final ExportContext context;

    private final LevelRepository levelRepository;

    private final NodeCacheService nodeCacheService;

    private final FundVersionRepository fundVersionRepository;

    private final UserService userService;

    private final EntityManager em;

    private final ResourcePathResolver resourcePathResolver;

    public SectionsReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.levelRepository = initHelper.getLevelRepository();
        this.nodeCacheService = initHelper.getNodeCacheService();
        this.fundVersionRepository = initHelper.getFundVersionRepository();
        this.userService = initHelper.getUserService();
        this.em = initHelper.getEm();
        this.resourcePathResolver = initHelper.getResourcePathResolver();
    }

    /**
     * Reads all funds sections. For each section will be opened new section output stream.<br>
     */
    @Override
    public void read() {
        if (context.getFundsSections() == null) {
            return;
        }
        // check global export permissions
        boolean globalPermission = userService.hasPermission(Permission.ADMIN)
                || userService.hasPermission(Permission.FUND_EXPORT_ALL);

        // export all fund sections
        for (FundSections fss : context.getFundsSections()) {
            // find fund version
            ArrFundVersion fundVersion = fundVersionRepository.findByIdWithFetchForExport(fss.getFundVersionId());
            if (fundVersion == null) {
                throw new DEExportException("Fund version not found, fundVersionId:" + fss.getFundVersionId());
            }
            // check fund permission
            if (!globalPermission) {
                Integer fundId = fundVersion.getFundId();
                if (!userService.hasPermission(Permission.FUND_EXPORT, fundId)) {
                    throw Authorization.createAccessDeniedException(Permission.FUND_EXPORT);
                }
            }
            // read fund sections
            readFundSections(fss, fundVersion);
        }
    }

    private void readFundSections(FundSections fss, ArrFundVersion fundVersion) {
        Collection<Integer> rootNodeIds = fss.getRootNodeIds();
        if (rootNodeIds == null || rootNodeIds.isEmpty()) {
            rootNodeIds = Collections.singleton(fundVersion.getRootNodeId());
        }
        if (fss.isMergeSections()) {
            readMergedSections(fundVersion, fss.getLevelInfoListener(), rootNodeIds);
        } else {
            rootNodeIds.forEach(nodeId -> readSection(fundVersion, fss.getLevelInfoListener(), nodeId));
        }
    }

    private void readMergedSections(ArrFundVersion fundVersion,
                                    LevelInfoListener levelInfoListener,
                                    Collection<Integer> rootNodeIds) {
        ArrChange lockChange = fundVersion.getLockChange();
        SectionContext sectionContext = new SectionContext(fundVersion, context, true,
                levelInfoListener, nodeCacheService, em,
                this.resourcePathResolver);
        try {
            // read sections levels
            for (Integer rootNodeId : rootNodeIds) {
                // read parent nodes up to root
                levelRepository.findAllParentsByNodeId(rootNodeId, lockChange, true).forEach(sectionContext::addLevel);
                // read subtree
                levelRepository.readLevelTree(rootNodeId, lockChange, false, (level, depth) -> sectionContext.addLevel(level));
            }

            sectionContext.processed();
        } finally {
            sectionContext.close();
        }
    }

    private void readSection(ArrFundVersion fundVersion, LevelInfoListener levelInfoListener, int rootNodeId) {
        ArrChange lockChange = fundVersion.getLockChange();
        SectionContext sectionContext = new SectionContext(fundVersion, context, false,
                levelInfoListener, nodeCacheService, em,
                this.resourcePathResolver);
        try {
            levelRepository.readLevelTree(rootNodeId, lockChange, false, (level, depth) -> sectionContext.addLevel(level));

            sectionContext.processed();
        } finally {
            sectionContext.close();
        }
    }
}
