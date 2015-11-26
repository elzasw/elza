package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;

import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDescItemTypeExt;


/**
 * Zpracování pravidel typů parametrů.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
public class DescItemTypesRules extends Rules {

    public DescItemTypesRules(Path ruleFile) {
        super(ruleFile);
    }


    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @param arrangementType        typ výstupu
     */
    // TODO: je nutné používat synchronized?
    public synchronized List<RulDescItemTypeExt> execute(final List<RulDescItemTypeExt> rulDescItemTypeExtList,
                                                         final RulArrangementType arrangementType)
            throws Exception {
        preExecute(); // kontrola nového souboru

        StatelessKieSession session = kbase.newStatelessKieSession();

        List<RulDescItemTypeExt> rulDescItemTypeExtResultList = new ArrayList<>();

        // přidání globálních proměnných
        session.setGlobal("results", rulDescItemTypeExtResultList);
        session.setGlobal("arrangementType", arrangementType);

        session.execute(rulDescItemTypeExtList);

        return rulDescItemTypeExtResultList;
    }

}
