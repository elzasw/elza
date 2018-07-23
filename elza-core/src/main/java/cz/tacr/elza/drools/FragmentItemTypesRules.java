package cz.tacr.elza.drools;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ApFragmentRule;
import cz.tacr.elza.domain.ApFragmentType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApFragmentRuleRepository;
import org.kie.api.runtime.StatelessKieSession;
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
public class FragmentItemTypesRules extends Rules {

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private ApFragmentRuleRepository fragmentRuleRepository;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @return seznam typů atributů odpovídající pravidlům
     */
    public synchronized List<RulItemTypeExt> execute(final ApFragmentType fragmentType,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                     final List<ApItem> items) throws Exception {
        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(ModelFactory.createFragmentItems(items));
        facts.addAll(rulDescItemTypeExtList);
        ApFragmentRule fragmentRule = fragmentRuleRepository.findByFragmentTypeAndRuleType(fragmentType, ApFragmentRule.RuleType.FRAGMENT_ITEMS);

        if (fragmentRule == null) {
            throw new ObjectNotFoundException("Nebyly nalezeny pravidla pro typ fragmentu: " + fragmentType.getCode(), BaseCode.INVALID_STATE);
        }

        Path path = resourcePathResolver.getDroolsFile(fragmentRule);
        StatelessKieSession session = createNewStatelessKieSession(path);
        session.execute(facts);
        return rulDescItemTypeExtList;
    }

}
