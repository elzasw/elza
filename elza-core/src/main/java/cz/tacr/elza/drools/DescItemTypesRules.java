package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.service.RuleService;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.drools.service.ScriptModelFactory;


/**
 * Zpracování pravidel typů parametrů.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 26.11.2015
 */
@Component
public class DescItemTypesRules extends Rules {

    @Autowired
    private ScriptModelFactory scriptModelFactory;
    @Autowired
    private RulesExecutor rulesExecutor;
    @Autowired
    private RuleService ruleService;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     */
    // TODO: je nutné používat synchronized?
    public synchronized List<RulItemTypeExt> execute(final ArrLevel level,
                                                     final ArrFundVersion version,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList)
            throws Exception
    {

    	LinkedList<Object> facts = new LinkedList<>();
    	facts.addAll(rulDescItemTypeExtList);

    	// prepare list of levels
		ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(level, version);

    	ModelFactory.addLevelWithParents(activeLevel, facts);

    	final RulRuleSet rulRuleSet = version.getRuleSet();

        Path path;
        List<RulArrangementRule> rulArrangementRules = arrangementRuleRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulArrangementRule.RuleType.ATTRIBUTE_TYPES);

        for (RulArrangementRule rulArrangementRule : rulArrangementRules) {
            path = Paths.get(rulesExecutor.getDroolsDir(rulArrangementRule.getPackage().getCode(), rulArrangementRule.getRuleSet().getCode()) + File.separator + rulArrangementRule.getComponent().getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.ATTRIBUTE_TYPES);
        for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            path = Paths.get(rulesExecutor.getDroolsDir(rulExtensionRule.getPackage().getCode(), rulExtensionRule.getArrangementExtension().getRuleSet().getCode()) + File.separator + rulExtensionRule.getComponent().getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        return rulDescItemTypeExtList;
    }

}
