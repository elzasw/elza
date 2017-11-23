package cz.tacr.elza.deimport.sections.context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.DEImportParams.ImportDirection;
import cz.tacr.elza.deimport.context.ObservableImport;
import cz.tacr.elza.deimport.sections.SectionProcessedListener;
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

/**
 * Context for new funds or subtrees of import node.
 */
public class SectionsContext {

    private static final Logger LOG = LoggerFactory.getLogger(SectionsContext.class);

    private final List<SectionProcessedListener> sectionProcessedListeners = new LinkedList<>();

    private final SectionStorageDispatcher storageDispatcher;

    private final ArrChange createChange;

    private final RegScope importScope;

    private final ImportPosition importPosition;

    private final StaticDataProvider staticData;

    private final ArrangementService arrangementService;

    private final InstitutionRepository institutionRepository;

    private final LevelRepository levelRepository;

    private ContextSection currentSection;

    public SectionsContext(SectionStorageDispatcher storageDispatcher,
                           ArrChange createChange,
                           RegScope importScope,
                           ImportPosition importPosition,
                           StaticDataProvider staticData,
                           ArrangementService arrangementService,
                           InstitutionRepository institutionRepository,
                           LevelRepository levelRepository) {
        this.storageDispatcher = storageDispatcher;
        this.createChange = createChange;
        this.importScope = importScope;
        this.importPosition = importPosition;
        this.staticData = staticData;
        this.arrangementService = arrangementService;
        this.institutionRepository = institutionRepository;
        this.levelRepository = levelRepository;
    }

    public ImportPosition getImportPostition() {
        return importPosition;
    }

    public void init(ObservableImport observableImport) {
        // NOP
    }

    public void registerSectionProcessedListener(SectionProcessedListener sectionProcessedListener) {
        sectionProcessedListeners.add(sectionProcessedListener);
    }

    public void beginSection(String ruleSetCode) {
        Validate.isTrue(currentSection == null);

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
        if (importPosition != null) {
            String fundRuleSetCode = importPosition.getFundVersion().getRuleSet().getCode();
            if (!ruleSetCode.equals(fundRuleSetCode)) {
                throw new DEImportException(
                        "Rule set must match with fund, subsection code:" + ruleSetCode + ", fund code:" + fundRuleSetCode);
            }
            section.setRootAdapter(new SubsectionRootAdapter(importPosition, createChange, levelRepository));
        }

        // set current section
        currentSection = section;
    }

    public void setFundInfo(FundInfo fundInfo) {
        Validate.notNull(currentSection);

        if (importPosition != null) {
            LOG.warn("Fund info will be ignored during subsection import");
        } else {
            currentSection.setRootAdapter(
                    createFundRootAdapter(fundInfo, currentSection.getRuleSystem(), currentSection.getCreateChange()));
        }
    }

    public ContextSection getCurrentSection() {
        Validate.notNull(currentSection);

        return currentSection;
    }

    public void endSection() {
        Validate.notNull(currentSection);

        // store all changes
        storeAll();

        // notify listeners
        List<SectionProcessedListener> listeners = new ArrayList<>(sectionProcessedListeners);
        listeners.forEach(l -> l.onSectionProcessed(currentSection));

        // close & clear current section
        currentSection.close();
        currentSection = null;
    }

    /* section context methods */

    void addNode(ArrNodeWrapper node, int depth) {
        storageDispatcher.addNode(node, depth);
    }

    void addLevel(ArrLevelWrapper level, int depth) {
        storageDispatcher.addLevel(level, depth);
    }

    void addDescItem(ArrDescItemWrapper descItem, int depth) {
        storageDispatcher.addDescItem(descItem, depth);
    }

    void addData(ArrDataWrapper data, int depth) {
        storageDispatcher.addData(data, depth);
    }

    void addNodeRegister(ArrNodeRegisterWrapper nodeRegister, int depth) {
        storageDispatcher.addNodeRegister(nodeRegister, depth);
    }

    /* internal methods */

    private void storeAll() {
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

        return new FundRootAdapter(fund, ruleSystem, createChange, fundInfo.getTr(), arrangementService);
    }

    public static class ImportPosition {

        private final ArrFundVersion fundVersion;

        private final ArrLevel parentLevel;

        private final ArrLevel targetLevel;

        private final ImportDirection direction;

        private Integer levelPosition;

        public ImportPosition(ArrFundVersion fundVersion, ArrLevel parentLevel, ArrLevel targetLevel, ImportDirection direction) {
            this.fundVersion = Validate.notNull(fundVersion);
            this.parentLevel = Validate.notNull(parentLevel);
            this.targetLevel = targetLevel;
            this.direction = Validate.notNull(direction);
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
