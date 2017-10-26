package cz.tacr.elza.dataexchange.output.sections;

import java.util.Collection;

import javax.persistence.EntityManager;

import cz.tacr.elza.aop.Authorization;
import cz.tacr.elza.dataexchange.output.DEExportException;
import cz.tacr.elza.dataexchange.output.DEExportParams.FundParams;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.context.ExportReader;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
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

    public SectionsReader(ExportContext context, ExportInitHelper initHelper) {
        this.context = context;
        this.levelRepository = initHelper.getLevelRepository();
        this.nodeCacheService = initHelper.getNodeCacheService();
        this.fundVersionRepository = initHelper.getFundVersionRepository();
        this.userService = initHelper.getUserService();
        this.em = initHelper.getEntityManager();
    }

    /**
     * Reads all funds sections. For each section will be opened new section output stream.<br>
     * <i>Note: Implementation is not optimized for big number of funds (version, fund and
     * institution is fetched per fund parameters).</i>
     */
    @Override
    public void read() {
        if (context.getFundsParams() == null) {
            return;
        }
        // check global export permissions
        boolean globalPermission = userService.hasPermission(Permission.ADMIN)
                || userService.hasPermission(Permission.FUND_EXPORT_ALL);

        // export all fund sections
        for (FundParams fp : context.getFundsParams()) {
            // find fund version
            ArrFundVersion fundVersion = fundVersionRepository.findOne(fp.getFundVersionId());
            if (fundVersion == null) {
                throw new DEExportException("Fund version not found, fundVersionId:" + fp.getFundVersionId());
            }
            // check fund permission
            if (!globalPermission) {
                Integer fundId = fundVersion.getFund().getFundId();
                if (!userService.hasPermission(Permission.FUND_EXPORT, fundId)) {
                    throw Authorization.createAccessDeniedException(Permission.FUND_EXPORT);
                }
            }
            // read fund sections
            Collection<Integer> rootNodeIds = fp.getRootNodeIds();
            if (rootNodeIds == null || rootNodeIds.isEmpty()) {
                readSection(new SectionContext(fundVersion, context));
            } else {
                rootNodeIds.forEach(id -> new SectionContext(fundVersion, id, context));
            }
        }
    }

    private void readSection(SectionContext sc) {
        SectionOutputStream os = context.getBuilder().openSectionOutputStream(sc);
        try {
            LevelBatchReader batchReader = new LevelBatchReader(context.getBatchSize(), os, nodeCacheService);
            levelRepository.iterateSubtree(sc.getRootNodeId(), sc.getLockChange(), level -> {
                // TODO: replace detach for stateless session
                em.detach(level);
                batchReader.addLevel(level);
            });
            batchReader.flush();

            PacketLoader packetLoader = new PacketLoader(em, context.getBatchSize());
            for (Integer packetId : sc.getPacketIds()) {
                packetLoader.addRequest(packetId, new PacketDispatcher(os, sc.getRuleSystem(), sc.getFund()));
            }
            packetLoader.flush();

            os.processed();
        } finally {
            os.close();
        }
    }
}
