package cz.tacr.elza.deimport.sections.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.DEImportParams.ImportDirection;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.context.ImportObserver;
import cz.tacr.elza.deimport.context.ImportPhaseChangeListener;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.IEventNotificationService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;

/**
 * Context for new funds or subtrees of import node.
 */
public class SectionsContext {

    private static final Logger LOG = LoggerFactory.getLogger(SectionsContext.class);

    private final Collection<ArrPacketWrapper> packetQueue = new ArrayList<>();

    private final SectionStorageDispatcher storageDispatcher;

    private final ArrChange createChange;

    private final RegScope importScope;

    private final ImportPosition importPosition;

    private final StaticDataProvider staticData;

    private final ArrangementService arrangementService;

    private final InstitutionRepository institutionRepository;

    private final IEventNotificationService eventNotificationService;

    private final LevelRepository levelRepository;

    private ContextSection currentSection;

    public SectionsContext(SectionStorageDispatcher storageDispatcher,
                           ArrChange createChange,
                           RegScope importScope,
                           ImportPosition importPosition,
                           StaticDataProvider staticData,
                           ArrangementService arrangementService,
                           InstitutionRepository institutionRepository,
                           IEventNotificationService eventNotificationService,
                           LevelRepository levelRepository) {
        this.storageDispatcher = storageDispatcher;
        this.createChange = createChange;
        this.importScope = importScope;
        this.importPosition = importPosition;
        this.staticData = staticData;
        this.arrangementService = arrangementService;
        this.institutionRepository = institutionRepository;
        this.eventNotificationService = eventNotificationService;
        this.levelRepository = levelRepository;
    }

    public boolean isSubsection() {
        return importPosition != null;
    }

    public void init(ImportObserver importObserver) {
        if (isSubsection()) {
            importObserver.registerPhaseChangeListener(new SubsectionPhaseEndListener());
        }
    }

    public void beginSection(String ruleSetCode) {
        Assert.isNull(currentSection);

        // find rule system
        if (StringUtils.isEmpty(ruleSetCode)) {
            throw new DEImportException("Rule set code is empty");
        }
        RuleSystem ruleSystem = staticData.getRuleSystems().getByRuleSetCode(ruleSetCode);
        if (ruleSystem == null) {
            throw new DEImportException("Rule set not found, code:" + ruleSetCode);
        }

        // create section
        ContextSection section = new ContextSection(this, createChange, ruleSystem, arrangementService);

        // set subsection root adapter when present
        if (isSubsection()) {
            String fundRuleSetCode = importPosition.getFundVersion().getRuleSet().getCode();
            if (!ruleSetCode.equals(fundRuleSetCode)) {
                throw new DEImportException(
                        "Rule set must match with fund, subsection code:" + ruleSetCode + ", fund code:" + fundRuleSetCode);
            }
            currentSection.setRootAdapter(new SubsectionRootAdapter(importPosition, createChange, levelRepository));
        }

        // set current section
        currentSection = section;
    }

    public void setFundInfo(FundInfo fundInfo) {
        Assert.notNull(currentSection);

        if (isSubsection()) {
            LOG.warn("Fund info will be ignored during subsection import");
        } else {
            currentSection.setRootAdapter(
                    createFundRootAdapter(fundInfo, currentSection.getRuleSystem(), currentSection.getCreateChange()));
        }
    }

    public ContextSection getCurrentSection() {
        Assert.notNull(currentSection);

        return currentSection;
    }

    public void endSection() {
        Assert.notNull(currentSection);

        storeAll();
        currentSection.close();
        currentSection = null;
    }

    /* section context methods */

    void addPacket(ArrPacketWrapper packet) {
        packetQueue.add(packet);
        if (packetQueue.size() >= storageDispatcher.getBatchSize()) {
            storePackets();
        }
    }

    void storePackets() {
        if (packetQueue.isEmpty()) {
            return;
        }
        storageDispatcher.getStorageManager().saveSectionPackets(packetQueue);
        packetQueue.clear();
    }

    void addNode(ArrNodeWrapper node, int depth) {
        storageDispatcher.addNode(node, depth);
    }

    void addLevel(ArrLevelWrapper level, int depth) {
        storageDispatcher.addLevel(level, depth);
    }

    void addDescItem(ArrDescItemWrapper descItem, ArrDataWrapper data, int depth) {
        storageDispatcher.addDescItem(descItem, depth);
        storageDispatcher.addData(data, depth);
    }

    void addNodeRegister(ArrNodeRegisterWrapper nodeRegister, int depth) {
        storageDispatcher.addNodeRegister(nodeRegister, depth);
    }

    /* internal methods */

    private void storeAll() {
        storePackets();
        storageDispatcher.dispatchAll();
    }

    private FundRootAdapter createFundRootAdapter(FundInfo fundInfo, RuleSystem ruleSystem, ArrChange createChange) {
        if (StringUtils.isBlank(fundInfo.getN())) {
            throw new DEImportException("Fund name must be set");
        }
        ParInstitution institution = institutionRepository.findByInternalCode(fundInfo.getIc());
        if (institution == null) {
            throw new DEImportException("Institution not found, internal code:" + fundInfo.getIc());
        }
        ArrFund fund = arrangementService.createFund(fundInfo.getN(), fundInfo.getC(), institution);
        arrangementService.addScopeToFund(fund, importScope);

        return new FundRootAdapter(fund, ruleSystem, createChange, fundInfo.getTr(), arrangementService, eventNotificationService);
    }

    /**
     * Listens for end of subsection.
     */
    // TODO: prenest mimo import, udelat interfrace pro volani z vnejsku
    private static class SubsectionPhaseEndListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            if (previousPhase == ImportPhase.SECTIONS) {
                SectionsContext sections = context.getSections();
                ArrFundVersion fundVer = sections.importPosition.getFundVersion();
                EventId event = EventFactory.createIdEvent(EventType.FUND_UPDATE, fundVer.getFund().getFundId());
                sections.eventNotificationService.publishEvent(event);
                return false;
            }
            return !ImportPhase.SECTIONS.isSubsequent(nextPhase);
        }
    }

    public static class ImportPosition {

        private final ArrFundVersion fundVersion;

        private final ArrLevel parentLevel;

        private final ArrLevel targetLevel;

        private final ImportDirection direction;

        private Integer levelPosition;

        public ImportPosition(ArrFundVersion fundVersion, ArrLevel parentLevel, ArrLevel targetLevel, ImportDirection direction) {
            this.fundVersion = Objects.requireNonNull(fundVersion);
            this.parentLevel = Objects.requireNonNull(parentLevel);
            this.targetLevel = targetLevel;
            this.direction = Objects.requireNonNull(direction);
        }

        public ArrFundVersion getFundVersion() {
            return fundVersion;
        }

        public ArrLevel getParentLevel() {
            return parentLevel;
        }

        public ArrLevel getTargetLevel() {
            return targetLevel;
        }

        public ImportDirection getDirection() {
            return direction;
        }

        public Integer getLevelPosition() {
            return levelPosition;
        }

        void setLevelPosition(Integer levelPosition) {
            this.levelPosition = levelPosition;
        }
    }
}
