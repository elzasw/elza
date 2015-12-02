package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.drools.model.DescItemVO;
import cz.tacr.elza.drools.model.VOLevel;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LevelRepository;


/**
 * Tovární třída pro objekty validačních skriptů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 1.12.2015
 */
@Component
public class ScriptModelFactory {

    @Autowired
    private LevelRepository levelRepository;
    @Autowired
    private DataRepository arrDataRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    /**
     * Vytvoří strukturu od výchozího levelu. Načte všechny jeho rodiče a přímé potomky.
     */
    public VOLevel createLevelStructure(final ArrLevel level, final ArrFindingAidVersion version) {
        Assert.notNull(level);
        Assert.notNull(version);

        List<ArrLevel> parents = levelRepository.findAllParentsByNodeAndVersion(level.getNode(), version);
        Set<ArrNode> nodes = new HashSet<>();
        nodes.add(level.getNode());

        VOLevel mainLevel = createLevel(level, version);

        VOLevel voParent = mainLevel;
        for (ArrLevel parent : parents) {
            VOLevel newParent = createLevel(parent, version);
            voParent.setParent(newParent);
            newParent.addChild(voParent);
            voParent = newParent;

            nodes.add(parent.getNode());
        }

        List<ArrLevel> childs = levelRepository.findByParentNode(level.getNode(), version.getLockChange());
        List<VOLevel> levelChilds = new ArrayList<>(childs.size());
        for (ArrLevel child : childs) {
            VOLevel voChild = createLevel(child, version);
            voChild.setParent(mainLevel);
            mainLevel.addChild(voChild);
            levelChilds.add(voChild);

            nodes.add(child.getNode());
        }

        mainLevel.setChilds(levelChilds);
        assignDescItems(voParent, version, nodes);


        return mainLevel;
    }

    /**
     * Pro kořenový level projde celou jeho strukturu a přiřadí na ni hodnoty atributů.
     *
     * @param rootLevel kořenový uzel verze
     * @param version   verze
     * @param nodes     seznam nodů, pro které se budou hledat atributy
     */
    public void assignDescItems(final VOLevel rootLevel, final ArrFindingAidVersion version, final Set<ArrNode> nodes) {
        Assert.notNull(rootLevel);
        if (rootLevel.getParent() != null) {
            throw new IllegalArgumentException("Zadaný uzel není root.");
        }


        List<ArrDescItem> descItems = descItemRepository.findByNodes(nodes, version.getLockChange());

        Map<Integer, List<ArrDescItem>> descItemsMap =
                ElzaTools.createGroupMap(descItems, p -> p.getNode().getNodeId());

        List<VOLevel> levels = new LinkedList<>();
        structureToList(rootLevel, levels);

        for (VOLevel level : levels) {
            List<ArrDescItem> levelDescItems = descItemsMap.get(level.getNodeId());
            level.setDescItems(createDescItems(levelDescItems));
        }
    }

    /**
     * Převede stromovou strukturu na seznam.
     *
     * @param rootLevel  kořen struktury
     * @param resultList seznam
     */
    private void structureToList(final VOLevel rootLevel, final List<VOLevel> resultList) {
        resultList.add(rootLevel);

        if (rootLevel.getChilds() != null) {
            for (VOLevel child : rootLevel.getChilds()) {
                structureToList(child, resultList);
            }
        }
    }

    /**
     * Vytvoří hodnoty atributu.
     *
     * @param descItems hodnoty atributu
     * @return seznam vo hodnot atributu
     */
    public List<DescItemVO> createDescItems(@Nullable final List<ArrDescItem> descItems) {
        if (descItems == null) {
            return Collections.EMPTY_LIST;
        }

        List<DescItemVO> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            result.add(createDescItem(descItem));
        }
        return result;
    }

    /**
     * Vytvoří hodnoty atributu, pro každý atribut je zavolána extension.
     *
     * @param descItems seznam hodnot atributů
     * @param extension funkce pro zadání dodatečných hodnot
     * @return seznam vo hodnot
     */
    public List<DescItemVO> createDescItems(final List<ArrDescItem> descItems, final Consumer<DescItemVO> extension) {
        Assert.notNull(descItems);
        Assert.notNull(extension);

        List<DescItemVO> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItemVO item = createDescItem(descItem);
            result.add(item);
            extension.accept(item);
        }
        return result;
    }

    /**
     * Vytvoří hodnotu atributu.
     *
     * @param descItem atribut
     * @return vo hodnota atributu
     */
    public DescItemVO createDescItem(final ArrDescItem descItem) {
        DescItemVO item = new DescItemVO();
        item.setDescItemId(descItem.getDescItemId());
        item.setType(descItem.getDescItemType().getCode());
        item.setSpecCode(descItem.getDescItemSpec() == null ? null : descItem.getDescItemSpec().getCode());

        return item;
    }

    /**
     * Vytvoří level.
     *
     * @param level   level
     * @param version verze levelu
     * @return vo level
     */
    public VOLevel createLevel(final ArrLevel level, final ArrFindingAidVersion version) {

        VOLevel result = new VOLevel();
        result.setNodeId(level.getNode().getNodeId());

        return result;
    }

}
