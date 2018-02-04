package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRule;
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
    private ResourcePathResolver resourcePathResolver;

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

    	//AvailableDescItems results = new AvailableDescItems();

        ;
        List<RulRule> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulRule.RuleType.ATTRIBUTE_TYPES);

        for (RulRule rulPackageRule : rulPackageRules) {
            Path path = resourcePathResolver.getDroolFile(rulPackageRule);
            StatelessKieSession session = createNewStatelessKieSession(path);
            //session.setGlobal("results", results);
            execute(session, facts);
        }

        //results.finalize();

        return rulDescItemTypeExtList;
    }

}
