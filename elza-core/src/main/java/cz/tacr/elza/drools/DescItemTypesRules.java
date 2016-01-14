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

import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.AvailableDescItems;
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
	
    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     */
    // TODO: je nutné používat synchronized?
    public synchronized List<RulDescItemTypeExt> execute(final ArrLevel level,
                                                         final ArrFindingAidVersion version,
                                                         final List<RulDescItemTypeExt> rulDescItemTypeExtList,                                                         
                                                         final Set<String> strategies)
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
    	
    	// Add strategies
    	facts.addAll(ModelFactory.createStrategies(strategies));
    	
    	final RulRuleSet rulRuleSet = version.getRuleSet();

    	AvailableDescItems results = new AvailableDescItems();      

        Path path;
        List<RulRule> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulRule.RuleType.ATTRIBUTE_TYPES);

        for (RulRule rulPackageRule : rulPackageRules) {
            path = Paths.get(RulesExecutor.ROOT_PATH + File.separator + rulPackageRule.getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            session.setGlobal("results", results);
            execute(session, facts);
        }
        
        results.finalize();

        return results.getDescItemTypes();
    }

}
