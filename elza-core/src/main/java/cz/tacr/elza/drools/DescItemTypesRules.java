package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.RulPackageRules;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;


/**
 * Zpracování pravidel typů parametrů.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
@Component
public class DescItemTypesRules extends Rules {

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @param arrangementType        typ výstupu
     */
    // TODO: je nutné používat synchronized?
    public synchronized List<RulDescItemTypeExt> execute(final List<RulDescItemTypeExt> rulDescItemTypeExtList,
                                                         final RulArrangementType arrangementType,
                                                         final RulRuleSet rulRuleSet,
                                                         final Set<String> strategies)
            throws Exception {


        List<RulDescItemTypeExt> rulDescItemTypeExtResultList = new ArrayList<>();

        Path path;
        List<RulPackageRules> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulPackageRules.RuleType.ATTRIBUTE_TYPES);

        for (RulPackageRules rulPackageRule : rulPackageRules) {
            path = Paths.get(RulesExecutor.ROOT_PATH + File.separator + rulPackageRule.getFilename());
            StatelessKieSession session = createNewStatelessKieSession(rulRuleSet, path);
            session.setGlobal("results", rulDescItemTypeExtResultList);
            session.setGlobal("strategies", strategies);
            session.setGlobal("arrangementType", arrangementType);
            execute(session, rulDescItemTypeExtList, path);
        }

        return rulDescItemTypeExtResultList;
    }

}
