package cz.tacr.elza.drools;

import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RulesConfigExecutor rulesConfigExecutor;

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

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(rulDescItemTypeExtList);
        facts.add(outputDefinition);
        facts.add(outputType);

        RulComponent component = outputType.getComponent();

        if (component == null) {
            logger.warn("Při vykonávání OutputItemTypesRules.execute() nebyly nalezeny pravidla pro typ výstupu '"
                    + outputType.getCode() + "'");
        } else {
            Path path = Paths.get(rulesConfigExecutor.getDroolsDir(outputType.getPackage().getCode(), outputType.getRuleSet().getCode()) + File.separator + component.getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        return rulDescItemTypeExtList;
    }

}
