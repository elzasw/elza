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
import cz.tacr.elza.domain.ArrDescItemEnum;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.model.ActiveLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.EventSource;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.drools.model.NewLevel;
import cz.tacr.elza.drools.model.NewLevelApproach;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
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
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

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

        Set<RulDescItemType> descItemTypesForPackets = descItemTypeRepository.findDescItemTypesForPackets();
        Set<RulDescItemType> descItemTypesForIntegers = descItemTypeRepository.findDescItemTypesForIntegers();

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

        DescItemReader descItemReader = new DescItemReader(descItemRepository, descItemTypeRepository, descItemFactory);
        
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
            	
    	DescItemReader descItemReader = new DescItemReader(descItemRepository, descItemTypeRepository, descItemFactory);
    	
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
    public ActiveLevel createActiveLevel(Level modelLevel,
                                         final ArrLevel level,
                                         final ArrFundVersion version) {
        DescItemReader descItemReader = new DescItemReader(descItemRepository, descItemTypeRepository, descItemFactory);

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
        return activeLevel;
    }
}
