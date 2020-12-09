package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
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
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.service.cache.NodeCacheService;


/**
 * Tovární třída pro objekty validačních skriptů.
 *
 */
@Component
public class ScriptModelFactory {

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private NodeCacheService nodeCacheService;

	@Autowired
	private StaticDataService staticDataService;

	@Autowired
	private StructuredItemRepository structItemRepos;

    /**
	 * Vytvoří strukturu od výchozího levelu. Načte všechny jeho rodiče a prvky
	 * popisu.
	 *
	 * @param descItemReader
	 *            Reader which will fetch description items from DB
	 */
	private Level createLevelModel(final ArrLevel level,
	        final ArrFundVersion version,
	        final DescItemReader descItemReader) {
        Assert.notNull(level, "Level musí být vyplněn");
        Assert.notNull(version, "Verze AS musí být vyplněna");

        List<ArrLevel> parents = levelRepository.findAllParentsByNodeId(level.getNodeId(), version.getLockChange(), false);
        Set<ArrNode> nodes = new HashSet<>();
		nodes.add(level.getNode());

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

        return mainLevel;
    }

	/**
	 * Create description item reader
	 *
	 * @param version
	 * @return
	 */
	private DescItemReader createDescItemReader(ArrFundVersion version) {

		DescItemReader descItemReader = new DescItemReader(version, descItemRepository,
		        descItemFactory,
		        nodeCacheService,
		        structItemRepos);
		return descItemReader;
	}

    /**
     * Vytvoří hodnoty atributu, pro každý atribut je zavolána extension.
     *
     * @param descItems seznam hodnot atributů
     * @param extension funkce pro zadání dodatečných hodnot
     * @return seznam vo hodnot
     */
    public List<DescItem> createDescItems(final List<ArrDescItem> descItems, final Consumer<DescItem> extension) {
        Assert.notNull(descItems, "Hodnoty atributů musí být vyplněny");
        Assert.notNull(extension, "Funkce pro zadání dodatečných hodnot musí být vyplněná");

        List<DescItem> result = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            DescItem item = DescItem.valueOf(descItem);
            result.add(item);
            extension.accept(item);
        }
        return result;
    }

    /**
	 * Vytvoření scénáře pro level z value objektu.
	 *
	 * @param newLevelApproach
	 *            scénář VO
	 * @param ruleSetId
	 *            ID of the ruleset
	 * @return
	 */
	public ScenarioOfNewLevel createScenarioOfNewLevel(final NewLevelApproach newLevelApproach, int ruleSetId) {
        StaticDataProvider sdp = staticDataService.getData();

        ScenarioOfNewLevel scenarioOfNewLevel = new ScenarioOfNewLevel();
        scenarioOfNewLevel.setName(newLevelApproach.getName());

        List<ArrDescItem> descItems = new ArrayList<>();
		for (DescItem descItemRules : newLevelApproach.getDescItems()) {
			ArrDescItem descItem = createDescItem(descItemRules, sdp);
			descItems.add(descItem);
        }

        scenarioOfNewLevel.setDescItems(descItems);

        return scenarioOfNewLevel;
    }

    /**
	 * Create new description item from value object
	 *
	 * @param descItemRule
	 *            Source description item
	 * @param sdp
	 * @return
	 */
	private ArrDescItem createDescItem(final DescItem descItemRule, StaticDataProvider sdp) {
		ItemType itemType = sdp.getItemTypeByCode(descItemRule.getType());
		Validate.notNull(itemType, "Item type: %d", descItemRule.getType());

        ArrDescItem descItem = new ArrDescItem();
		descItem.setItemType(itemType.getEntity());

		if (descItemRule.getSpecCode() != null) {
			RulItemSpec rulDescItemSpec = itemType.getItemSpecByCode(descItemRule.getSpecCode());
			Validate.notNull(rulDescItemSpec, "Item specification does not exists: %s", descItemRule.getSpecCode());
            descItem.setItemSpec(rulDescItemSpec);
        }

        if (!descItemRule.isUndefined()) {
            // set initial value
			if (descItemRule.getInteger() != null && itemType.getDataType() == DataType.INT) {
                ArrDataInteger data = new ArrDataInteger();
                data.setValue(descItemRule.getInteger());
                descItem.setData(data);
            } else {
				// This is strange?
				// Probably there should be data according item type
                ArrDataNull data = new ArrDataNull();
                descItem.setData(data);
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
		DescItemReader descItemReader = createDescItemReader(version);

		Level srcModelLevel = createLevelModel(level, version, descItemReader);

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
		ModelFactory.addLevelWithParents(newLevel, (List) levels);

        // Read description items
		descItemReader.read();

        return levels;
    }

    /**
	 * Create active level model for given level
	 * @return
	 */
	public ActiveLevel createActiveLevel(
                                         final ArrLevel level,
	        final ArrFundVersion version) {

		DescItemReader descItemReader = createDescItemReader(version);

		// prepare list of levels
		Level modelLevel = createLevelModel(level, version, descItemReader);

        ActiveLevel activeLevel = new ActiveLevel(modelLevel);

        // read descitems for this activelevel instead of original level
		descItemReader.add(activeLevel, level.getNode());

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

		descItemReader.read();

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
		Validate.notNull(descItemLevel);
        while (tmpLevel != null) {
            // atributy procházeného rodiče
            List<DescItem> descItems = tmpLevel.getDescItems();

            // atributy, které v cyklu jsou označeny jako effektivní
            List<DescItem> effectiveDescItemsAdd = new ArrayList<>();

            // existuje nějaký atribut?
			if (descItemLevel != null && descItemLevel.size() > 0) {
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
