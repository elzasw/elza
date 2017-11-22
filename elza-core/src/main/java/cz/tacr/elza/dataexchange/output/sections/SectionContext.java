package cz.tacr.elza.dataexchange.output.sections;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.utils.HibernateUtils;

public class SectionContext {

    private final Set<Integer> processedPacketIds = new HashSet<>();

    private final Set<Integer> processedNodeIds = new HashSet<>();

    private final LevelInfoLoader levelInfoLoader;

    private final PacketLoader packetLoader;

    private final ArrFundVersion fundVersion;

    private final ExportContext context;

    /**
     * If true then processedNodeIds is used as filter for overlap nodes.
     */
    private final boolean multipleSections;

    private final RuleSystem ruleSystem;

    private final ExportLevelInfoListener levelInfoListener;

    private final EntityManager em;

    private SectionOutputStream outputStream;

    SectionContext(ArrFundVersion fundVersion,
                   ExportContext context,
                   boolean multipleSections,
                   ExportLevelInfoListener levelInfoListener,
                   NodeCacheService nodeCacheService,
                   EntityManager em) {
        this.levelInfoLoader = new LevelInfoLoader(context.getBatchSize(), nodeCacheService);
        this.packetLoader = new PacketLoader(em, context.getBatchSize());
        this.fundVersion = Validate.notNull(fundVersion);
        this.context = Validate.notNull(context);
        this.multipleSections = multipleSections;
        this.ruleSystem = context.getStaticData().getRuleSystems().getByRuleSetId(fundVersion.getRuleSetId());
        this.levelInfoListener = levelInfoListener;
        this.em = em;
    }

    public ExportContext getContext() {
        return context;
    }

    public String getInstitutionCode() {
        return fundVersion.getFund().getInstitution().getInternalCode();
    }

    public String getFundName() {
        return fundVersion.getFund().getName();
    }

    public String getFundInternalCode() {
        return fundVersion.getFund().getInternalCode();
    }

    public String getTimeRange() {
        return fundVersion.getDateRange();
    }

    public RuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public void addPacketId(Integer packetId) {
        Validate.notNull(packetId);

        if (!processedPacketIds.add(packetId)) {
            return; // packet already processed
        }

        PacketDispatcher packetDispatcher = new PacketDispatcher(getOutputStream(), ruleSystem, fundVersion.getFund());
        packetLoader.addRequest(packetId, packetDispatcher);
    }

    /* internal methods */

    void addLevel(ArrLevel level) {
        Validate.isTrue(HibernateUtils.isInitialized(level));

        if (multipleSections && !processedNodeIds.add(level.getNodeId())) {
            return; // node already processed
        }

        em.detach(level); // TODO: replace with stateless session

        LevelInfoDispatcher levelInfoDispatcher = new LevelInfoDispatcher(getOutputStream(), levelInfoListener);
        levelInfoLoader.addRequest(level, levelInfoDispatcher);
    }

    /**
     * Flushes loaders and closes output stream.
     */
    void processed() {
        levelInfoLoader.flush();
        packetLoader.flush();
        if (outputStream != null) {
            outputStream.processed();
        }
    }

    /**
     * Closes output stream.
     */
    void close() {
        if (outputStream != null) {
            outputStream.close();
        }
    }

    /* private methods */

    private SectionOutputStream getOutputStream() {
        if (outputStream == null) {
            outputStream = context.getBuilder().openSectionOutputStream(this);
        }
        return outputStream;
    }
}
