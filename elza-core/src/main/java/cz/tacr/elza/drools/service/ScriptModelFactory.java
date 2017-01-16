package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.EventSource;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevel;
import cz.tacr.elza.drools.model.NewLevelApproach;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;


/**
 * Tovární třída pro objekty validačních skriptů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @author Petr Pytelka [<a href="mailto:petr.pytelka@lightcomp.cz">petr.pytelka@lightcomp.cz</a>]
 * @since 1.12.2015
 */
@Component
public class ScriptModelFactory {

    @Autowired
    private LevelRepository levelRepository;

    /*
    @Autowired
    private DataRepository arrDataRepository;
     */

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private DescItemRepository descItemRepository;

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
            /*
            if (tmp instanceof NewLevel) {
                NewLevel newLevel = (NewLevel) tmp;
                if (newLevel.getSiblingAfter() != null) {
                    list.add(newLevel.getSiblingAfter());
                }
                if (newLevel.getSiblingBefore() != null) {
                    list.add(newLevel.getSiblingBefore());
                }
            }*/
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
            return Collections.emptyList();
        }

        Set<RulItemType> descItemTypesForPackets = itemTypeRepository.findDescItemTypesForPackets();
        Set<RulItemType> descItemTypesForIntegers = itemTypeRepository.findDescItemTypesForIntegers();

        return ModelFactory.createDescItems(descItems, descItemTypesForPackets, descItemTypesForIntegers, descItemFactory);
    }

    /**
     * Vytvoří strukturu od výchozího levelu. Načte všechny jeho rodiče a prvky popisu.
     */
    public Level createLevelModel(final ArrLevel level,
                                        final ArrFundVersion version) {
        Assert.notNull(level);
        Assert.notNull(version);

        List<ArrLevel> parents = levelRepository.findAllParentsByNodeAndVersion(level.getNode(), version);
        Set<ArrNode> nodes = new HashSet<>();
        nodes.add(level.getNode());

        DescItemReader descItemReader = new DescItemReader(descItemRepository, itemTypeRepository, descItemFactory);

        Level mainLevel = ModelFactory.createLevel(level, version);
        descItemReader.add(mainLevel, level.getNode());

        Level voParent = mainLevel;
        for (ArrLevel parent : parents) {
            Level newParent = ModelFactory.createLevel(parent, version);
            voParent.setParent(newParent);
            newParent.setHasChildren(true);
            voParent = newParent;

            descItemReader.add(newParent, parent.getNode());
        }

        descItemReader.read(version);

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
            DescItem item = ModelFactory.createDescItem(descItem);
            result.add(item);
            extension.accept(item);
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
            descItems.add(createDescItem(descItemVO));
        }

        scenarioOfNewLevel.setDescItems(descItems);

        return scenarioOfNewLevel;
    }

    /**
     * Create new description item from value object
     * @param descItemVO Source description item
     * @return
     */
    private ArrDescItem createDescItem(final DescItem descItemVO) {
        RulItemType rulDescItemType = itemTypeRepository.getOneByCode(descItemVO.getType());
        Assert.notNull(rulDescItemType, "Item does not exists: " + descItemVO.getType());

        ArrDescItem descItem = new ArrDescItem(descItemFactory.createItemByType(rulDescItemType.getDataType()));
        descItem.setItemType(rulDescItemType);

        // set specification
        ArrItemData item = descItem.getItem();
        if (descItemVO.getSpecCode() != null) {
            RulItemSpec rulDescItemSpec = itemSpecRepository.getOneByCode(descItemVO.getSpecCode());
            Assert.notNull(rulDescItemSpec, "Item specification does not exists: " + descItemVO.getSpecCode());
            descItem.setItemSpec(rulDescItemSpec);
        }

        // set initial value
        if (descItemVO.getInteger() != null)
        {
            if(item instanceof ArrItemInt) {
                // Set initial value
                ((ArrItemInt) item).setValue(descItemVO.getInteger());
            } else {
                throw new IllegalStateException("Initial value cannot be set for item: " + descItemVO.getType());
            }
        }
        return descItem;
    }


    /**
     * Vytvoří fakta pro nově přidávaný level. Načte jeho sousedy.
     * @param level             referenční level
     * @param directionLevel    způsob přídání
     * @param version           verze AP
     *
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Level> createFactsForNewLevel(
                                   final ArrLevel level,
                                   final DirectionLevel directionLevel, final ArrFundVersion version)
    {
        Level srcModelLevel = createLevelModel(level, version);

        DescItemReader descItemReader = new DescItemReader(descItemRepository, itemTypeRepository, descItemFactory);

        // Parent level
        Level parentLevel = null;
        EventSource eventSource = null;
        Level modelSiblingBefore = null;
        Level modelSiblingAfter = null;

        List<ArrLevel> siblings;
        ArrLevel siblingBefore;
        ArrLevel siblingAfter;
        int indexOfMainLevel;

        // Prepare data for new level
        switch (directionLevel) {
        case BEFORE:
            siblings = levelRepository.findByParentNode(level.getNodeParent(), version.getLockChange());
            if (srcModelLevel != null) {
                parentLevel = srcModelLevel.getParent();
            }
            eventSource = EventSource.SIBLING_AFTER;

            indexOfMainLevel = siblings.indexOf(level);
            if (indexOfMainLevel > 0) {
                siblingBefore = siblings.get(indexOfMainLevel - 1);
                modelSiblingBefore = ModelFactory.createLevel(siblingBefore, version);
                descItemReader.add(modelSiblingBefore, siblingBefore.getNode());
            }

            siblingAfter = level;
            modelSiblingAfter = ModelFactory.createLevel(siblingAfter, version);
            descItemReader.add(modelSiblingAfter, siblingAfter.getNode());

            break;
        case AFTER:
            siblings = levelRepository.findByParentNode(level.getNodeParent(), version.getLockChange());
            if (srcModelLevel != null) {
                parentLevel = srcModelLevel.getParent();
            }
            eventSource = EventSource.SIBLING_BEFORE;

            siblingBefore = level;
            modelSiblingBefore = ModelFactory.createLevel(siblingBefore, version);
            descItemReader.add(modelSiblingBefore, siblingBefore.getNode());

            indexOfMainLevel = siblings.indexOf(level);
            if (indexOfMainLevel < siblings.size() - 1) {
                siblingAfter = siblings.get(indexOfMainLevel + 1);
                modelSiblingAfter = ModelFactory.createLevel(siblingAfter, version);
                descItemReader.add(modelSiblingAfter, siblingAfter.getNode());
            }

            break;

        case CHILD:
            parentLevel = srcModelLevel;
            eventSource = EventSource.PARENT;

            List<ArrLevel> childs = levelRepository.findByParentNode(level.getNode(), version.getLockChange());
            if (childs.size() > 0) {
                siblingBefore = childs.get(childs.size() - 1);
                modelSiblingBefore = ModelFactory.createLevel(siblingBefore, version);
                descItemReader.add(modelSiblingBefore, siblingBefore.getNode());
            }
            break;

        case ROOT:
            // pokud je vytvářena AP, tak se root level vytváří automaticky
            break;

        default:
            throw new IllegalStateException("Nedefinovaný stav DirectionLevel");

        }

        // Prepare new level
        NewLevel newLevel = new NewLevel();
        newLevel.setParent(parentLevel);
        newLevel.setEventSource(eventSource);
        newLevel.setSiblingAfter(modelSiblingAfter);
        newLevel.setSiblingBefore(modelSiblingBefore);

        // Add to the output
        List<Level> levels = new LinkedList<>();
        ModelFactory.addAll(newLevel, (List) levels);

        // Read description items
        descItemReader.read(version);

        return levels;
    }

    /**
     * Create active level for given level
     * @param modelLevel prepared model level
     * @return
     */
    public ActiveLevel createActiveLevel(final Level modelLevel,
                                         final ArrLevel level,
                                         final ArrFundVersion version) {
        DescItemReader descItemReader = new DescItemReader(descItemRepository, itemTypeRepository, descItemFactory);

        ActiveLevel activeLevel = new ActiveLevel(modelLevel);

        // Prepare children info
        Integer childsCount = levelRepository.countChildsByParent(level.getNode(), version.getLockChange());
        activeLevel.setChildCount(childsCount);

        // Prepare siblinds
        List<ArrLevel> siblings;
        siblings = levelRepository.findByParentNode(level.getNodeParent(), version.getLockChange());

        // Find siblings
        Level modelSiblingBefore = null;
        Level modelSiblingAfter = null;
        ArrLevel siblingBefore = null;
        //ArrLevel siblingAfter ;
        for(Iterator<ArrLevel> it = siblings.iterator(); it.hasNext(); )
        {
            ArrLevel l = it.next();
            if(l.getNode().getNodeId().equals(modelLevel.getNodeId())) {
                if(siblingBefore!=null) {
                    modelSiblingBefore = ModelFactory.createLevel(siblingBefore, version);
                    descItemReader.add(modelSiblingBefore, siblingBefore.getNode());
                }
                if(it.hasNext()) {
                    ArrLevel siblingAfter = it.next();
                    modelSiblingAfter = ModelFactory.createLevel(siblingAfter, version);
                    descItemReader.add(modelSiblingAfter, siblingAfter.getNode());
                }
                break;
            }
            siblingBefore = l;
        }

        activeLevel.setSiblingAfter(modelSiblingAfter);
        activeLevel.setSiblingBefore(modelSiblingBefore);

        // Read description items
        descItemReader.read(version);

        // Add effective attributes
        addEffectiveDescItems(activeLevel);

        return activeLevel;
    }

    /**
     * Přidání efektivních atributů.
     *
     * @param level požadovaný level, ke kterému se budou efektivní atributu vytvářet
     */
    private void addEffectiveDescItems(final Level level) {
        Level tmpLevel = level.getParent();
        List<DescItem> descItemLevel = level.getDescItems();
        while (tmpLevel != null) {
            // atributy procházeného rodiče
            List<DescItem> descItems = tmpLevel.getDescItems();

            // atributy, které v cyklu jsou označeny jako effektivní
            List<DescItem> effectiveDescItemsAdd = new ArrayList<>();

            // existuje nějaký atribut?
            if (descItemLevel.size() > 0) {
                for (DescItem descItem : descItems) {
                    String dataType = descItem.getDataType();
                    Boolean addAsEffective = true;

                    // u typu ENUM se neporovnává specifikace (protože je to hodnota)
                    if (dataType.equals("ENUM")) {
                        addAsEffective = compareAttributesForEnum(descItemLevel, descItem, addAsEffective);
                    } else {
                        addAsEffective = compareAttributesForOther(descItemLevel, descItem, addAsEffective);
                    }

                    // pokud ještě neexistuje, přidá se do seznamu pro přidání
                    if (addAsEffective) {
                        addDescItemToEffective(tmpLevel, effectiveDescItemsAdd, descItem);
                    }
                }
            } else {
                // pokud neexistuje žádný atribut, přidají se všechny z procházeného předka
                for (DescItem descItem : descItems) {
                    addDescItemToEffective(tmpLevel, effectiveDescItemsAdd, descItem);
                }
            }

            // přidání nových efektivních atributů z procházeného předka
            descItemLevel.addAll(effectiveDescItemsAdd);
            tmpLevel = tmpLevel.getParent();
        }
    }

    /**
     * Přidání hodnoty atributu mezi efektivní.
     *
     * @param level     procházený level
     * @param descItems seznam atributů
     * @param descItem  přidávaný atribut
     */
    private void addDescItemToEffective(final Level level, final List<DescItem> descItems, final DescItem descItem) {
        DescItem descItemCopy = new DescItem(descItem);
        // vyplní se nodeId - stane se z něj effektivní atribut
        descItemCopy.setNodeId(level.getNodeId());
        descItems.add(descItemCopy);
    }

    /**
     * Porovnání atributů pro typ ENUM
     *
     * @param descItems       seznam atributů k porovnání
     * @param descItemCompare porovnávaný astribut
     * @param addAsEffective  přidat jako effektivní atribut?
     * @return přidat jako effektivní atribut
     */
    private boolean compareAttributesForEnum(final List<DescItem> descItems, final DescItem descItemCompare, boolean addAsEffective) {
        for (DescItem descItem : descItems) {
            if (descItem.getType().equals(descItemCompare.getType())) {
                addAsEffective = false;
                break;
            }
        }
        return addAsEffective;
    }

    /**
     * Porovnání atributů pro ostatní typy
     *
     * @param descItems       seznam atributů k porovnání
     * @param descItemCompare porovnávaný astribut
     * @param addAsEffective  přidat jako effektivní atribut?
     * @return přidat jako effektivní atribut
     */
    private boolean compareAttributesForOther(final List<DescItem> descItems, final DescItem descItemCompare, boolean addAsEffective) {
        for (DescItem descItem : descItems) {
            if (descItem.getType().equals(descItemCompare.getType())
                    && ((descItem.getSpecCode() != null &&
                    descItem.getSpecCode().equals(descItemCompare.getSpecCode())) ||
                    descItem.getSpecCode() == null && descItem.getSpecCode() == descItemCompare.getSpecCode()
            )) {
                addAsEffective = false;
                break;
            }
        }
        return addAsEffective;
    }
}
