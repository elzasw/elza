package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ObservableImport;
import cz.tacr.elza.dataexchange.input.sections.SectionProcessedListener;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.service.ArrangementService;

/**
 * Context for new funds or subtrees of import node.
 */
public class SectionsContext {

    private static final Logger LOG = LoggerFactory.getLogger(SectionsContext.class);

    private final List<SectionProcessedListener> sectionProcessedListeners = new LinkedList<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final ArrChange createChange;

    private final ApScope importScope;

    private final ImportPosition importPosition;

    private final StaticDataProvider staticData;

    private final ImportInitHelper initHelper;

    private SectionContext currentSection;

    public SectionsContext(StorageManager storageManager, int batchSize, ArrChange createChange, ApScope importScope,
            ImportPosition importPosition, StaticDataProvider staticData, ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.createChange = createChange;
        this.importScope = importScope;
        this.importPosition = importPosition;
        this.staticData = staticData;
        this.initHelper = initHelper;
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

    /**
     * Prepare context for new section.
     *
     * Method endSection have to be called when section is finished.
     *
     * @param ruleSetCode
     *            Rules for section
     */
    public void beginSection(String ruleSetCode) {
        Validate.isTrue(currentSection == null);

        // find rule system
        if (StringUtils.isEmpty(ruleSetCode)) {
            throw new DEImportException("Rule set code is empty");
        }
        RulRuleSet ruleSet = staticData.getRuleSetByCode(ruleSetCode);
        if (ruleSet == null) {
            throw new DEImportException("Rule set not found, code:" + ruleSetCode);
        }

        // create current section
        SectionContext section = new SectionContext(storageManager, batchSize, createChange,
                                                    ruleSet, staticData, initHelper);

        // set subsection root adapter when position present
        if (importPosition != null) {
            String fundRuleSetCode = importPosition.getFundVersion().getRuleSet().getCode();
            if (!ruleSetCode.equals(fundRuleSetCode)) {
                throw new DEImportException("Rule set must match with fund, subsection code:" + ruleSetCode
                        + ", fund code:" + fundRuleSetCode);
            }
            section.setRootAdapter(new SubsectionRootAdapter(importPosition, createChange, initHelper));
        }

        // set current section
        currentSection = section;
    }

    public void setFundInfo(FundInfo fundInfo) {
        Validate.notNull(currentSection);

        if (importPosition != null) {
            LOG.warn("Fund info will be ignored during subsection import");
        } else {
            prepareNewFundRootAdapter(fundInfo, currentSection);
        }
    }

    public SectionContext getCurrentSection() {
        Validate.notNull(currentSection);

        return currentSection;
    }

    public void endSection() {
        Validate.notNull(currentSection);

        currentSection.storeNodes();

        // notify listeners
        List<SectionProcessedListener> listeners = new ArrayList<>(sectionProcessedListeners);
        listeners.forEach(l -> l.onSectionProcessed(currentSection));

        // close & clear current section
        currentSection.close();
        currentSection = null;
    }

    /* internal methods */

    private void prepareNewFundRootAdapter(FundInfo fundInfo, SectionContext sectionCtx) {
        InstitutionRepository instRepo = initHelper.getInstitutionRepository();
        ArrangementService arrService = initHelper.getArrangementService();

        if (StringUtils.isBlank(fundInfo.getN())) {
            throw new DEImportException("Fund name must be set");
        }
        ParInstitution institution = instRepo.findByInternalCode(fundInfo.getIc());
        if (institution == null) {
            throw new DEImportException("Institution not found, internal code:" + fundInfo.getIc());
        }
        ArrFund fund = arrService.createFund(fundInfo.getN(), fundInfo.getC(), institution, null, null, null);
        arrService.addScopeToFund(fund, importScope);

        FundRootAdapter adapter = new FundRootAdapter(fund,
                                   sectionCtx.getRuleSet(),
                                   sectionCtx.getCreateChange(),
                                   fundInfo.getTr(),
                                   arrService);
        currentSection.setRootAdapter(adapter);
    }
}
