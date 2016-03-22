package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.Level;
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

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     */
    // TODO: je nutné používat synchronized?
    public synchronized List<RulDescItemTypeExt> execute(final ArrLevel level,
                                                         final ArrFundVersion version,
                                                         final List<RulDescItemTypeExt> rulDescItemTypeExtList)
            throws Exception
    {

    	LinkedList<Object> facts = new LinkedList<>();
    	facts.addAll(rulDescItemTypeExtList);

    	// prepare list of levels
    	Level modelLevel = scriptModelFactory.createLevelModel(level, version);
    	ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(modelLevel, level, version);
    	ModelFactory.addAll(activeLevel, facts);

    	// Add arrangement type
    	RulArrangementType arrangementType = version.getArrangementType();
    	facts.add(arrangementType);

    	final RulRuleSet rulRuleSet = version.getRuleSet();

    	//AvailableDescItems results = new AvailableDescItems();

        Path path;
        List<RulRule> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulRule.RuleType.ATTRIBUTE_TYPES);

        for (RulRule rulPackageRule : rulPackageRules) {
            path = Paths.get(rulesExecutor.getRootPath() + File.separator + rulPackageRule.getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            //session.setGlobal("results", results);
            execute(session, facts);
        }

        //results.finalize();

        return rulDescItemTypeExtList;
    }

}
