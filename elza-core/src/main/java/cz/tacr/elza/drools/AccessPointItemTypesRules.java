package cz.tacr.elza.drools;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApRuleRepository;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;


/**
 * Zpracování pravidel typů parametrů pro fragment.
 *
 * @since 03.11.2017
 */
@Component
public class AccessPointItemTypesRules extends Rules {

    public static final Logger logger = LoggerFactory.getLogger(AccessPointItemTypesRules.class);

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private ApRuleRepository ruleRepository;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @return seznam typů atributů odpovídající pravidlům
     */
    public synchronized List<RulItemTypeExt> execute(final ApType type,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                     final List<ApItem> items,
                                                     final ApRule.RuleType ruleType) throws Exception {
        if (type.getRuleSystem() == null) {
            logger.warn("Typ {} nemá pravidla pro strukturovaný popis", type.getCode());
            return rulDescItemTypeExtList;
        }
        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(ModelFactory.createApItems(items));
        facts.addAll(rulDescItemTypeExtList);
        ApRule rule = ruleRepository.findByRuleSystemAndRuleType(type.getRuleSystem(), ruleType);

        if (rule == null) {
            throw new ObjectNotFoundException("Nebyly nalezeny pravidla pro typ: " + type.getCode(), BaseCode.INVALID_STATE)
                    .set("ruleType", ruleType);
        }

        Path path = resourcePathResolver.getDroolsFile(rule);
        KieSession session = createKieSession(path);
        executeSession(session, facts);
        return rulDescItemTypeExtList;
    }

}
