package cz.tacr.elza.dataexchange.output.sections;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.service.cache.NodeCacheService;

public class SectionContext {

    private final Set<Integer> processedStructObjectIds = new HashSet<>();

    private final Set<Integer> processedNodeIds = new HashSet<>();

    private final Set<Integer> processedFileIds = new HashSet<>();

    private final ExportContext context;

    private final StaticDataProvider staticData;

    private final LevelInfoLoader levelInfoLoader;

    private final StructObjectInfoLoader structObjLoader;

    private final DmsFileLoader dmsFileLoader;

    private final ArrFundVersion fundVersion;

    /**
     * If true then processedNodeIds is used as filter for overlap nodes.
     */
    private final boolean multipleSections;

    private final LevelInfoListener levelInfoListener;

    private final EntityManager em;

    private SectionOutputStream outputStream;

    SectionContext(ArrFundVersion fundVersion,
                   ExportContext context,
                   boolean multipleSections,
                   LevelInfoListener levelInfoListener,
                   NodeCacheService nodeCacheService,
                   EntityManager em,
                   ResourcePathResolver resourceResolver) {
        this.context = Validate.notNull(context);
        this.staticData = context.getStaticData();
        this.levelInfoLoader = new LevelInfoLoader(context.getBatchSize(), nodeCacheService);
        this.structObjLoader = new StructObjectInfoLoader(em, context.getBatchSize(), this.staticData);
        this.dmsFileLoader = new DmsFileLoader(em, context.getBatchSize(), resourceResolver);
        this.fundVersion = Validate.notNull(fundVersion);
        this.multipleSections = multipleSections;
        this.levelInfoListener = levelInfoListener;
        this.em = em;
    }

    public ExportContext getContext() {
        return context;
    }

    public StaticDataProvider getStaticData() {
        return staticData;
    }
    
    public RulRuleSet getRuleSet() {
        return staticData.getRuleSetById(fundVersion.getRuleSetId());
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

    public void addStructObjectId(Integer structObjId) {
        Validate.notNull(structObjId);

        if (!processedStructObjectIds.add(structObjId)) {
            return; // object already processed
        }

        StructObjectInfoDispatcher structObjDispatcher = new StructObjectInfoDispatcher(getOutputStream());
        structObjLoader.addRequest(structObjId, structObjDispatcher);
    }

    public void addDmsFile(Integer fileId) {
        Validate.notNull(fileId);

        if (!processedFileIds.add(fileId)) {
            return; // object already processed
        }

        DmsFileDispatcher dmsFileDispatcher = new DmsFileDispatcher(getOutputStream());
        dmsFileLoader.addRequest(fileId, dmsFileDispatcher);
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
        structObjLoader.flush();
        dmsFileLoader.flush();
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
