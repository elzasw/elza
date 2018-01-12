package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRule;


/**
 * Zpracování pravidel typů parametrů.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Component
public class OutputItemTypesRules extends Rules {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(rulDescItemTypeExtList);
        facts.add(outputDefinition);
        facts.add(outputDefinition.getOutputType());

        RulRule rule = outputDefinition.getOutputType().getRule();

        if (rule == null) {
            logger.warn("Při vykonávání OutputItemTypesRules.execute() nebyly nalezeny pravidla pro typ výstupu '"
                    + outputDefinition.getOutputType().getCode() + "'");
        } else {
            if (!rule.getRuleType().equals(RulRule.RuleType.OUTPUT_ATTRIBUTE_TYPES)) {
                throw new IllegalStateException("Neplatný typ pravidel pro výstup: " + rule.getRuleType().name());
            }

            Path path = resourcePathResolver.getDroolFile(rule);
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        return rulDescItemTypeExtList;
    }

}
