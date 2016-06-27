package cz.tacr.elza.drools;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;


/**
 * Zpracování pravidel typů parametrů.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Component
public class OutputItemTypesRules extends Rules {

    @Autowired
    private RulesExecutor rulesExecutor;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     */
    public synchronized List<RulItemTypeExt> execute(final ArrOutputDefinition outputDefinition,
                                                     final ArrFundVersion version,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList)
            throws Exception
    {

    	LinkedList<Object> facts = new LinkedList<>();
    	facts.addAll(rulDescItemTypeExtList);
        facts.add(outputDefinition);
        facts.add(outputDefinition.getOutputType());

    	final RulRuleSet rulRuleSet = version.getRuleSet();

        Path path;
        List<RulRule> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulRule.RuleType.OUTPUT_ATTRIBUTE_TYPES);

        for (RulRule rulPackageRule : rulPackageRules) {
            path = Paths.get(rulesExecutor.getRootPath() + File.separator + rulPackageRule.getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        return rulDescItemTypeExtList;
    }

}
