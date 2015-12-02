package cz.tacr.elza.drools;

import java.util.ArrayList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.stereotype.Component;

import cz.tacr.elza.api.vo.RuleEvaluationType;
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
                                                         final RuleEvaluationType evaluationType)
            throws Exception {
        StatelessKieSession session = createNewStatelessKieSession(rulRuleSet);

        List<RulDescItemTypeExt> rulDescItemTypeExtResultList = new ArrayList<>();

        // přidání globálních proměnných
        session.setGlobal("results", rulDescItemTypeExtResultList);
        session.setGlobal("evaluationType", evaluationType);
        session.setGlobal("arrangementType", arrangementType);

        execute(session, rulRuleSet, rulDescItemTypeExtList);

        return rulDescItemTypeExtResultList;
    }

    @Override
    protected String getFileName() {
        return "types" + RulesExecutor.FILE_EXTENSION;
    }
}
