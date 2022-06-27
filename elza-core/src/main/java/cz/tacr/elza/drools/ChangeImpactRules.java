package cz.tacr.elza.drools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.DescItemChange;
import cz.tacr.elza.drools.service.ScriptModelFactory;
import cz.tacr.elza.service.RuleService;


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

    @Autowired
    private RuleService ruleService;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param createDescItem
     *            hodnoty atributů k vytvoření
     * @param updateDescItem
     *            hodnoty atributů k upravení
     * @param deleteDescItem
     *            hodnoty atributů ke smazání
     * @param nodeTypeOperation
     *            typ operace
     * @return seznam dopadů
     * @throws IOException
     */
    public synchronized Set<RelatedNodeDirection> execute(final List<ArrDescItem> createDescItem,
                                                          final List<ArrDescItem> updateDescItem,
                                                          final List<ArrDescItem> deleteDescItem,
                                                          final NodeTypeOperation nodeTypeOperation,
                                                          final ArrFundVersion version)
            throws IOException
    {
        StaticDataProvider sdp = staticDataService.getData();
        RuleSet ruleSet = sdp.getRuleSetById(version.getRuleSetId());
        List<RulArrangementRule> rulArrangementRules = ruleSet.getRulesByType(
                                                                              RulArrangementRule.RuleType.CONFORMITY_IMPACT);

        List<Object> facts = new LinkedList<>();
        facts.addAll( prepareDescItemList(createDescItem, updateDescItem, deleteDescItem) );
        facts.add(nodeTypeOperation);

        Set<RelatedNodeDirection> relatedNodeDirections = new HashSet<>();

        for (RulArrangementRule rulArrangementRule : rulArrangementRules) {
            Path path = resourcePathResolver.getDroolFile(rulArrangementRule);

            KieSession session = createKieSession(path);

            // přidání globálních proměnných
            session.setGlobal("results", relatedNodeDirections);

            executeSession(session, facts);
        }

        // TODO ELZA-1558: jak?
        /*List<RulExtensionRule> rulExtensionRules = ruleService.findExtensionRuleByNode(level.getNode(), RulExtensionRule.RuleType.CONFORMITY_INFO);
        for (RulExtensionRule rulExtensionRule : rulExtensionRules) {
            path = Paths.get(rulesExecutor.getDroolsDir(rulExtensionRule.getPackage().getCode(), rulExtensionRule.getArrangementExtension().getRuleSet().getCode()) + File.separator + rulExtensionRule.getComponent().getFilename());

            StatelessKieSession session = createKieSession(path);

            // přidání globálních proměnných
            session.setGlobal("results", relatedNodeDirections);

            execute(session, facts);
        }*/

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
