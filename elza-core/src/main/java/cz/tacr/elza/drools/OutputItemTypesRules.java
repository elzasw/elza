package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;


/**
 * Zpracování pravidel typů parametrů.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Component
public class OutputItemTypesRules extends Rules {

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     */
    public synchronized List<RulItemTypeExt> execute(final ArrOutputDefinition outputDefinition,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList)
            throws Exception
    {
        
        RulOutputType outputType = outputDefinition.getOutputType();

        RulOutputType outputType = outputDefinition.getOutputType();

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(rulDescItemTypeExtList);
        facts.add(outputDefinition);
        facts.add(outputType);

        Path path = resourcePathResolver.getDroolFile(outputType);
        } else {
            StatelessKieSession session = createNewStatelessKieSession(path);
        session.execute(facts);

        return rulDescItemTypeExtList;
    }

}
