package cz.tacr.elza.drools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.domain.RulPackageRules;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.drools.model.DescItemChange;
import cz.tacr.elza.drools.model.DescItemVO;
import cz.tacr.elza.drools.service.ScriptModelFactory;


/**
 * Zpracování pravidel dopadu změny na stavy uzlu.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
@Component
public class ImpactOfChangesLevelStateRules extends Rules {

    @Autowired
    private ScriptModelFactory factory;


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
        List<DescItemVO> descItemVOList = prepareDescItemVOList(createDescItem, updateDescItem, deleteDescItem);

        Set<RelatedNodeDirection> relatedNodeDirections = new HashSet<>();

        Path path;
        List<RulPackageRules> rulPackageRules = packageRulesRepository.findByRuleSetAndRuleTypeOrderByPriorityAsc(
                rulRuleSet, RulPackageRules.RuleType.CONFORMITY_IMPACT);

        for (RulPackageRules rulPackageRule : rulPackageRules) {
            path = Paths.get(RulesExecutor.ROOT_PATH + File.separator + rulPackageRule.getFilename());

            StatelessKieSession session = createNewStatelessKieSession(rulRuleSet, path);

            // přidání globálních proměnných
            session.setGlobal("results", relatedNodeDirections);
            session.setGlobal("nodeTypeOperation", nodeTypeOperation);

            execute(session, descItemVOList, path);
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
    private List<DescItemVO> prepareDescItemVOList(final List<ArrDescItem> createDescItem,
                                                   final List<ArrDescItem> updateDescItem,
                                                   final List<ArrDescItem> deleteDescItem) {
        List<DescItemVO> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(createDescItem)) {
            list.addAll(factory.createDescItems(createDescItem, (t)->t.setChange(DescItemChange.CREATE)));
        }

        if (!CollectionUtils.isEmpty(updateDescItem)) {
            factory.createDescItems(updateDescItem, (t) -> t.setChange(DescItemChange.UPDATE));
        }

        if (!CollectionUtils.isEmpty(deleteDescItem)) {
            factory.createDescItems(deleteDescItem, (t) -> t.setChange(DescItemChange.DELETE));
        }

        return list;
    }


}
