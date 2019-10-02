package cz.tacr.elza.dataexchange.input.sections.context;

import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.service.ArrangementService;

/**
 * Adapter to import section as new fund
 *
 */
class FundRootAdapter implements SectionRootAdapter {

    private final ArrFund fund;

    private final RulRuleSet ruleSet;

    private final ArrChange createChange;

    private final String timeRange;

    private final ArrangementService arrangementService;

    private ArrFundVersion fundVersion;

    /**
     * Flag if root was created
     */
    boolean rootCreated = false;

    public FundRootAdapter(ArrFund fund,
            RulRuleSet ruleSet,
            ArrChange createChange,
            String timeRange,
            ArrangementService arrangementService) {
        this.fund = fund;
        this.ruleSet = ruleSet;
        this.createChange = createChange;
        this.timeRange = timeRange;
        this.arrangementService = arrangementService;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void onSectionClose() {
        if (fundVersion == null) {
            throw new DEImportException("Root level not found, fund name:" + fund.getName());
        }
    }

    @Override
    public NodeContext createRoot(SectionContext contextSection, ArrNode rootNode, String importNodeId) {
        // check root
        if (rootCreated) {
            throw new DEImportException("Section must have only one root, levelId:" + importNodeId);
        }
        rootCreated = true;

        ArrNodeWrapper nodeWrapper = new ArrNodeWrapper(rootNode) {
            @Override
            public void afterEntitySave(Session session) {
                // fund version requires rootNode, 
                // can be created only after persist of root node
                fundVersion = arrangementService.createVersion(createChange, fund, ruleSet, rootNode, timeRange);
                super.afterEntitySave(session);
            }
        };

        ArrLevelWrapper levelWrapper = NodeContext.createLevelWrapper(nodeWrapper.getIdHolder(), null, 1, createChange);

        return contextSection.addNode(nodeWrapper, levelWrapper, importNodeId, 0);
    }
}
