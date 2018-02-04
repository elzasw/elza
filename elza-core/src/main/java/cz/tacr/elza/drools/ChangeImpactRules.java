package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulRule;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.DescItemChange;
import cz.tacr.elza.drools.service.ScriptModelFactory;


/**
 * Zpracování pravidel dopadu změny na stavy uzlu.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 27.11.2015
 */
@Component
public class ChangeImpactRules extends Rules {

    @Autowired
    private ScriptModelFactory factory;

    @Autowired
    private ResourcePathResolver resourcePathResolver;


    /**
     * Spuštění zpracování pravidel.
     *
     * @param createDescItem    hodnoty atributů k vytvoření
     * @param updateDescItem    hodnoty atributů k upravení
     * @param deleteDescItem    hodnoty atributů ke smazání
     * @param nodeTypeOperation typ operace
     * @return seznam dopadů
     */
    public synchronized Set<RelatedNodeDirection> execute(final List<ArrDescItem> createDescItem,
                                                          final List<ArrDescItem> updateDescItem,
                                                          final List<ArrDescItem> deleteDescItem,
                                                          final NodeTypeOperation nodeTypeOperation,
                                                          final RulRuleSet rulRuleSet)
            throws Exception {
        List<Object> facts = new LinkedList<>();
        facts.addAll( prepareDescItemList(createDescItem, updateDescItem, deleteDescItem) );
        facts.add(nodeTypeOperation);

        Set<RelatedNodeDirection> relatedNodeDirections = new HashSet<>();

        List<RulRule> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulRule.RuleType.CONFORMITY_IMPACT);

        for (RulRule rulPackageRule : rulPackageRules) {
            Path path = resourcePathResolver.getDroolFile(rulPackageRule);

            StatelessKieSession session = createNewStatelessKieSession(path);

            // přidání globálních proměnných
            session.setGlobal("results", relatedNodeDirections);

            execute(session, facts);
        }
        return relatedNodeDirections;
    }

    /**
     * Konverze hodnot atributů na VO do modelu.
     * @param createDescItem hodnoty atributů k vytvoření
     * @param updateDescItem hodnoty atributů k upravení
     * @param deleteDescItem hodnoty atributů ke smazání
     * @return seznam VO do modelu
     */
    private List<DescItem> prepareDescItemList(final List<ArrDescItem> createDescItem,
                                                   final List<ArrDescItem> updateDescItem,
                                                   final List<ArrDescItem> deleteDescItem) {
        List<DescItem> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(createDescItem)) {
            list.addAll(factory.createDescItems(createDescItem, (t)->t.setChange(DescItemChange.CREATE)));
        }

        if (!CollectionUtils.isEmpty(updateDescItem)) {
        	list.addAll(factory.createDescItems(updateDescItem, (t) -> t.setChange(DescItemChange.UPDATE)));
        }

        if (!CollectionUtils.isEmpty(deleteDescItem)) {
        	list.addAll(factory.createDescItems(deleteDescItem, (t) -> t.setChange(DescItemChange.DELETE)));
        }

        return list;
    }


}
