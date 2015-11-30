package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.drools.model.DescItemChange;
import cz.tacr.elza.drools.model.DescItemVO;


/**
 * Zpracování pravidel dopadu změny na stavy uzlu.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public class ImpactOfChangesLevelStateRules extends Rules {

    public ImpactOfChangesLevelStateRules(Path ruleFile) {
        super(ruleFile);
    }


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
                                                          final NodeTypeOperation nodeTypeOperation)
            throws Exception {
        preExecute(); // kontrola nového souboru

        List<DescItemVO> descItemVOList = prepareDescItemVOList(createDescItem, updateDescItem, deleteDescItem);

        StatelessKieSession session = kbase.newStatelessKieSession();

        Set<RelatedNodeDirection> relatedNodeDirections = new HashSet<>();

        // přidání globálních proměnných
        session.setGlobal("results", relatedNodeDirections);
        session.setGlobal("nodeTypeOperation", nodeTypeOperation);

        session.execute(descItemVOList);

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
            for (ArrDescItem descItem : createDescItem) {
                DescItemVO item = new DescItemVO();
                item.setChange(DescItemChange.CREATE);
                item.setType(descItem.getDescItemType().getCode());
                list.add(item);
            }
        }

        if (!CollectionUtils.isEmpty(updateDescItem)) {
            for (ArrDescItem descItem : updateDescItem) {
                DescItemVO item = new DescItemVO();
                item.setChange(DescItemChange.UPDATE);
                item.setType(descItem.getDescItemType().getCode());
                list.add(item);
            }
        }

        if (!CollectionUtils.isEmpty(deleteDescItem)) {
            for (ArrDescItem descItem : deleteDescItem) {
                DescItemVO item = new DescItemVO();
                item.setChange(DescItemChange.DELETE);
                item.setType(descItem.getDescItemType().getCode());
                list.add(item);
            }
        }

        return list;
    }

}
