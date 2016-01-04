package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import cz.tacr.elza.domain.ArrDescItemEnum;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPacketRef;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.EventSource;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevel;
import cz.tacr.elza.drools.model.Packet;
import cz.tacr.elza.drools.model.NewLevelApproach;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
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
    private DescItemFactory descItemFactory;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    /**
     * Vytvoří strukturu od výchozího levelu. Načte všechny jeho rodiče a přímé potomky.
     */
    public Level createLevelStructureWithChilds(final ArrLevel level, final ArrFindingAidVersion version) {
        Assert.notNull(level);
        Assert.notNull(version);

        List<ArrLevel> parents = levelRepository.findAllParentsByNodeAndVersion(level.getNode(), version);
        Set<ArrNode> nodes = new HashSet<>();
        nodes.add(level.getNode());

        Level mainLevel = createLevel(level, version);

        Level voParent = mainLevel;
        for (ArrLevel parent : parents) {
            Level newParent = createLevel(parent, version);
            voParent.setParent(newParent);
            newParent.setChildCount(1);
            voParent = newParent;

            nodes.add(parent.getNode());
        }


        Integer childsCount = levelRepository.countChildsByParent(level.getNode(), version.getLockChange());
        mainLevel.setChildCount(childsCount);

        assignDescItems(mainLevel, version, nodes);


        return mainLevel;
    }

    /**
     * Pro kořenový level projde celou jeho strukturu a přiřadí na ni hodnoty atributů.
     *
     * @param mainLevel level, pro který je struktura sestavena
     * @param version   verze
     * @param nodes     seznam nodů, pro které se budou hledat atributy
     */
    public void assignDescItems(final Level mainLevel, final ArrFindingAidVersion version, final Set<ArrNode> nodes) {
        Assert.notNull(mainLevel);


        List<ArrDescItem> descItems = descItemRepository.findByNodes(nodes, version.getLockChange());

        Map<Integer, List<ArrDescItem>> descItemsMap =
                ElzaTools.createGroupMap(descItems, p -> p.getNode().getNodeId());

        List<Level> levels = convertLevelTreeToList(mainLevel);

        for (Level level : levels) {
            List<ArrDescItem> levelDescItems = descItemsMap.get(level.getNodeId());
            level.setDescItems(createDescItems(levelDescItems));
        }
    }

    /**
     * Převede stromovou strukturu na seznam.
     *
     * @param level  level, pro který je struktura sestavena
     * @return resultList seznam
     */
    public List<Level> convertLevelTreeToList(final Level level) {
        List<Level> list = new ArrayList<>();
        Level tmp = level;
        while (tmp != null) {
            list.add(tmp);
            if (tmp instanceof NewLevel) {
                NewLevel newLevel = (NewLevel) tmp;
                if (newLevel.getSiblingAfter() != null) {
                    list.add(newLevel.getSiblingAfter());
                }
                if (newLevel.getSiblingBefore() != null) {
                    list.add(newLevel.getSiblingBefore());
                }
            }
            tmp = tmp.getParent();
        }
        return list;
    }

    /**
     * Vytvoří hodnoty atributu.
     *
     * @param descItems hodnoty atributu
     * @return seznam vo hodnot atributu
     */
    public List<DescItem> createDescItems(@Nullable final List<ArrDescItem> descItems) {
        if (descItems == null) {
            return Collections.EMPTY_LIST;
        }

        Set<RulDescItemType> descItemTypesForPackets = descItemTypeRepository.findDescItemTypesForPackets();
        Set<RulDescItemType> descItemTypesForIntegers = descItemTypeRepository.findDescItemTypesForIntegers();

        List<DescItem> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItem voDescItem = createDescItem(descItem);
            result.add(voDescItem);

            if (descItemTypesForPackets.contains(descItem.getDescItemType())) {
                ArrDescItemPacketRef packetRef = (ArrDescItemPacketRef) descItemFactory.getDescItem(descItem);

                ArrPacket packet = packetRef.getPacket();
                if (packet != null) {
                    voDescItem.setPacket(createPacket(packet));
                }
            } else if (descItemTypesForIntegers.contains(descItem.getDescItemType())) {
                ArrDescItemInt integer = (ArrDescItemInt) descItemFactory.getDescItem(descItem);
                voDescItem.setInteger(integer.getValue());
            }
        }

        return result;
    }

    /**
     * Vytvoří strukturu od výchozího levelu. Načte všechny jeho rodiče a předchozího a dalšího sourozence.
     */
    public Level createLevelStructure(final ArrLevel level,
                                        final ArrFindingAidVersion version) {
        Assert.notNull(level);
        Assert.notNull(version);

        List<ArrLevel> parents = levelRepository.findAllParentsByNodeAndVersion(level.getNode(), version);
        Set<ArrNode> nodes = new HashSet<>();
        nodes.add(level.getNode());

        Level mainLevel = createLevel(level, version);

        Level voParent = mainLevel;
        for (ArrLevel parent : parents) {
            Level newParent = createLevel(parent, version);
            voParent.setParent(newParent);
            newParent.setChildCount(1);
            voParent = newParent;

            nodes.add(parent.getNode());
        }

        assignDescItems(mainLevel, version, nodes);

        return mainLevel;
    }
    
    /**
     * Vytvoří hodnoty atributu, pro každý atribut je zavolána extension.
     *
     * @param descItems seznam hodnot atributů
     * @param extension funkce pro zadání dodatečných hodnot
     * @return seznam vo hodnot
     */
    public List<DescItem> createDescItems(final List<ArrDescItem> descItems, final Consumer<DescItem> extension) {
        Assert.notNull(descItems);
        Assert.notNull(extension);

        List<DescItem> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItem item = createDescItem(descItem);
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
    public DescItem createDescItem(final ArrDescItem descItem) {
        DescItem item = new DescItem();
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
    public Level createLevel(final ArrLevel level, final ArrFindingAidVersion version) {

        Level result = new Level();
        result.setNodeId(level.getNode().getNodeId());

        return result;
    }

    private Packet createPacket(final ArrPacket packet) {

        Packet result = new Packet();
        result.setStorageNumber(packet.getStorageNumber());
        result.setInvalidPacket(packet.getInvalidPacket());

        if (packet.getPacketType() != null) {
            RulPacketType packetType = packet.getPacketType();
            Packet.VOPacketType voPacketType = new Packet.VOPacketType();
            voPacketType.setCode(packetType.getCode());
            voPacketType.setName(packetType.getName());
            voPacketType.setShortcut(packetType.getShortcut());
            result.setPacketType(voPacketType);
        }
        return result;
    }

    /**
     * Vytvoření scénáře pro level z value objektu.
     * @param newLevelApproach  scénář VO
     * @return
     */
    public ScenarioOfNewLevel createScenarioOfNewLevel(final NewLevelApproach newLevelApproach) {
        ScenarioOfNewLevel scenarioOfNewLevel = new ScenarioOfNewLevel();
        scenarioOfNewLevel.setName(newLevelApproach.getName());

        List<ArrDescItem> descItems = new ArrayList<>();
        for (DescItem descItemVO : newLevelApproach.getDescItems()) {
            RulDescItemType rulDescItemType = descItemTypeRepository.getOneByCode(descItemVO.getType());
            Assert.notNull(rulDescItemType);
            ArrDescItem descItem = descItemFactory.createDescItemByType(rulDescItemType.getDataType());
            descItem.setDescItemType(rulDescItemType);

            if (descItemVO.getSpecCode() != null) {
                RulDescItemSpec rulDescItemSpec = descItemSpecRepository.getOneByCode(descItemVO.getSpecCode());
                Assert.notNull(rulDescItemSpec);
                descItem.setDescItemSpec(rulDescItemSpec);
            } else if (descItemVO.getInteger() != null && descItem instanceof ArrDescItemInt) {
                ((ArrDescItemInt) descItem).setValue(descItemVO.getInteger());
            } else if (descItem instanceof ArrDescItemEnum) {
                // ok
            } else {
                throw new IllegalStateException("Není definována konverze " + descItemVO + " a " + descItem.getClass().toString());
            }

            descItems.add(descItem);
        }

        scenarioOfNewLevel.setDescItems(descItems);

        return scenarioOfNewLevel;
    }

    /**
     * Vytvoří nově přidávaného levelu. Načte jeho sousedy.
     *
     * @param level             referenční level
     * @param directionLevel    způsob přídání
     * @param version           verze AP
     * @return
     */
    public NewLevel createNewLevel(final ArrLevel level,
                                   final DirectionLevel directionLevel,
                                   final ArrFindingAidVersion version) {

        NewLevel newLevel = new NewLevel();
        Set<ArrNode> nodes = new HashSet<>();

        List<ArrLevel> siblings;
        ArrLevel siblingBefore;
        ArrLevel siblingAfter;
        Level voSiblingBefore;
        Level voSiblingAfter;
        int indexOfMainLevel;

        siblings = levelRepository.findByParentNode(level.getNodeParent(), version.getLockChange());

        switch (directionLevel) {
            case BEFORE:
                newLevel.setEventSource(EventSource.SIBLING_AFTER);

                indexOfMainLevel = siblings.indexOf(level);
                if (indexOfMainLevel > 0) {
                    siblingBefore = siblings.get(indexOfMainLevel - 1);
                    voSiblingBefore = createLevel(siblingBefore, version);
                    nodes.add(siblingBefore.getNode());
                    newLevel.setSiblingBefore(voSiblingBefore);
                }

                siblingAfter = level;
                voSiblingAfter = createLevel(siblingAfter, version);
                nodes.add(siblingAfter.getNode());
                newLevel.setSiblingAfter(voSiblingAfter);

                break;
            case AFTER:
                newLevel.setEventSource(EventSource.SIBLING_BEFORE);

                siblingBefore = level;
                voSiblingBefore = createLevel(siblingBefore, version);
                nodes.add(siblingBefore.getNode());
                newLevel.setSiblingBefore(voSiblingBefore);

                indexOfMainLevel = siblings.indexOf(level);
                if (indexOfMainLevel < siblings.size() - 1) {
                    siblingAfter = siblings.get(indexOfMainLevel + 1);
                    voSiblingAfter = createLevel(siblingAfter, version);
                    nodes.add(siblingAfter.getNode());
                    newLevel.setSiblingAfter(voSiblingAfter);
                }

                break;

            case CHILD:
                newLevel.setEventSource(EventSource.PARENT);

                List<ArrLevel> childs = levelRepository.findByParentNode(level.getNode(), version.getLockChange());
                if (childs.size() > 0) {
                    siblingBefore = childs.get(childs.size() - 1);
                    voSiblingBefore = createLevel(siblingBefore, version);
                    nodes.add(siblingBefore.getNode());
                    newLevel.setSiblingBefore(voSiblingBefore);
                }
                break;

            case ROOT:
                // pokud je vytvářena AP, tak se root level vytváří automaticky
                break;

            default:
                throw new IllegalStateException("Nedefinovaný stav DirectionLevel");
        }

        assignDescItems(newLevel, version, nodes);

        return newLevel;
    }
}
