package cz.tacr.elza.dataexchange.input.sections.context;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.sections.context.ContextSection.SectionRootAdapter;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.service.ArrangementService;

class FundRootAdapter implements SectionRootAdapter {

    private final ArrFund fund;

    private final RuleSystem ruleSystem;

    private final ArrChange createChange;

    private final String timeRange;

    private final ArrangementService arrangementService;

    private ArrFundVersion fundVersion;

    public FundRootAdapter(ArrFund fund,
                           RuleSystem ruleSystem,
                           ArrChange createChange,
                           String timeRange,
                           ArrangementService arrangementService) {
        this.fund = fund;
        this.ruleSystem = ruleSystem;
        this.createChange = createChange;
        this.timeRange = timeRange;
        this.arrangementService = arrangementService;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public ArrNodeWrapper createNodeWrapper(ArrNode rootNode) {
        return new ArrNodeWrapper(rootNode) {
            @Override
            public void afterEntityPersist() {
                RulRuleSet ruleSet = ruleSystem.getRuleSet();
                fundVersion = arrangementService.createVersion(createChange, fund, ruleSet, rootNode, timeRange);
                super.afterEntityPersist();
            }
        };
    }

    @Override
    public ArrLevelWrapper createLevelWrapper(EntityIdHolder<ArrNode> rootNodeIdHolder) {
        return ContextNode.createLevelWrapper(rootNodeIdHolder, null, 1, createChange);
    }

    @Override
    public void onSectionClose() {
        if (fundVersion == null) {
            throw new DEImportException("Root level not found, fund name:" + fund.getName());
        }
    }
}
