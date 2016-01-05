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

import cz.tacr.elza.domain.RulPackageRules;
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
     * @param arrangementType        typ výstupu
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
    	
    	Level modelLevel = scriptModelFactory.createLevelStructure(level, version);
    	ActiveLevel activeLevel = scriptModelFactory.createActiveLevel(modelLevel, level, version);
    	ModelFactory.addAll(activeLevel, facts);
    	
    	RulArrangementType arrangementType = version.getArrangementType();
    	final RulRuleSet rulRuleSet = version.getRuleSet();

    	AvailableDescItems results = new AvailableDescItems();      

        Path path;
        List<RulPackageRules> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulPackageRules.RuleType.ATTRIBUTE_TYPES);

        for (RulPackageRules rulPackageRule : rulPackageRules) {
            path = Paths.get(RulesExecutor.ROOT_PATH + File.separator + rulPackageRule.getFilename());
            StatelessKieSession session = createNewStatelessKieSession(rulRuleSet, path);
            session.setGlobal("results", results);
            session.setGlobal("strategies", strategies);
            session.setGlobal("arrangementType", arrangementType);
            execute(session, facts, path);
        }
        
        results.finalize();

        return results.getDescItemTypes();
    }

}
