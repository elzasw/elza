package cz.tacr.elza.service;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Serviska pro správu hodnot atributů.
 *
 * @author Martin Šlapa
 * @since 13. 1. 2016
 */
@Service
public class DescriptionItemService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private EventNotificationService notificationService;

    /**
     * Kontrola otevřené verze.
     *
     * @param version verze
     */
    private void checkFindingAidVersionLock(final ArrFindingAidVersion version) {
        if (version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node uzel
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node) {
        node.setLastUpdate(LocalDateTime.now());
        nodeRepository.save(node);
        nodeRepository.flush();
        return node;
    }

    /**
     * Smaže hodnotu atributu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param descItemObjectId    identifikátor hodnoty atributu
     * @param nodeVersion         verze uzlu (optimistické zámky)
     * @param findingAidVersionId identifikátor verze archivní pomůcky
     * @return smazaná hodnota atributu
     */
    public ArrDescItem deleteDescriptionItem(final Integer descItemObjectId,
                                             final Integer nodeVersion,
                                             final Integer findingAidVersionId) {
        Assert.notNull(descItemObjectId);
        Assert.notNull(nodeVersion);
        Assert.notNull(findingAidVersionId);

        ArrChange change = arrangementService.createChange();
        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(findingAidVersionId);
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemObjectId);

        if (descItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (descItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }

        ArrDescItem descItem = descItems.get(0);
        descItem.getNode().setVersion(nodeVersion);

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(descItem.getNode());

        ArrDescItem descItemDeleted = deleteDescriptionItem(descItem, findingAidVersion, change, true);

        // uložení poslední uživatelské změny nad AP k verzi AP
        arrangementService.saveLastChangeFaVersion(change, findingAidVersion);

        // validace uzlu
        ruleService.conformityInfo(findingAidVersionId, Arrays.asList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, null, Arrays.asList(descItem));

        return descItemDeleted;
    }


    /**
     * Smaže hodnoty atributu podle typu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param findingAidVersionId   identifikátor verze archivní pomůcky
     * @param nodeId                identifikátor uzlu
     * @param nodeVersion           verze uzlu (optimistické zámky)
     * @param descItemTypeId        identifikátor typu hodnoty atributu
     * @return  upravený uzel
     */
    public ArrNode deleteDescriptionItemsByType(final Integer findingAidVersionId,
                                                final Integer nodeId,
                                                final Integer nodeVersion,
                                                final Integer descItemTypeId) {

        ArrChange change = arrangementService.createChange();
        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(findingAidVersionId);
        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(findingAidVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        ArrNode node = nodeRepository.findOne(nodeId);
        node.setVersion(nodeVersion);

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(node);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemType, node);

        if (descItems.size() == 0) {
            throw new IllegalStateException("Nebyla nalezena žádná hodnota atributu ke smazání");
        }

        List<ArrDescItem> descItemsDeleted = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            descItemsDeleted.add(deleteDescriptionItem(descItem, findingAidVersion, change, false));
        }

        // uložení poslední uživatelské změny nad AP k verzi AP
        arrangementService.saveLastChangeFaVersion(change, findingAidVersion);

        // validace uzlu
        ruleService.conformityInfo(findingAidVersionId, Arrays.asList(node.getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, null, descItemsDeleted);

        return node;
    }

    /**
     * Vytvoření hodnoty atributu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param descItem              hodnota atributu
     * @param nodeId                identifikátor uzlu
     * @param nodeVersion           verze uzlu (optimistické zámky)
     * @param findingAidVersionId   identifikátor verze archivní pomůcky
     * @return vytvořená hodnota atributu
     */
    public ArrDescItem createDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeId,
                                             final Integer nodeVersion,
                                             final Integer findingAidVersionId) {
        Assert.notNull(descItem);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);
        Assert.notNull(findingAidVersionId);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(findingAidVersionId);

        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node);

        // uložení uzlu (kontrola optimistických zámků)
        node.setVersion(nodeVersion);
        saveNode(node);

        return createDescriptionItem(descItem, node, version, null);
    }

    /**
     * Vytvoření hodnoty atributu. Při ukládání nedojde ke zvýšení verze uzlu.
     * - se spuštěním validace uzlu
     *
     * @param descItem hodnota atributu
     * @param node     uzel, kterému přidáme hodnotu
     * @param version  verze stromu
     * @return vytvořená hodnota atributu
     */
    public ArrDescItem createDescriptionItem(final ArrDescItem descItem,
                                             final ArrNode node,
                                             final ArrFindingAidVersion version,
                                             @Nullable final ArrChange createChange) {

        ArrChange change = createChange == null ? arrangementService.createChange() : createChange;

        descItem.setNode(node);
        descItem.setCreateChange(change);
        descItem.setDeleteChange(null);
        descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        ArrDescItem descItemCreated = createDescriptionItemWithData(descItem, version, change);

        // uložení poslední uživatelské změny nad AP k verzi AP
        arrangementService.saveLastChangeFaVersion(change, version.getFindingAidVersionId());

        // validace uzlu
        ruleService.conformityInfo(version.getFindingAidVersionId(), Arrays.asList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, Arrays.asList(descItem), null, null);

        // sockety
        publishChangeDescItem(version, descItemCreated);

        return descItemCreated;
    }

    /**
     * Vytvoření hodnoty atributu s daty.
     *
     * @param descItem hodnota atributu
     * @param version  verze archivní pomůcky
     * @param change   změna operace
     * @return vytvořená hodnota atributu
     */
    public ArrDescItem createDescriptionItemWithData(final ArrDescItem descItem,
                                                     final ArrFindingAidVersion version,
                                                     final ArrChange change) {
        Assert.notNull(descItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro vytváření musí být verze otevřená
        checkFindingAidVersionLock(version);

        // kontrola validity typu a specifikace
        checkValidTypeAndSpec(descItem);

        int maxPosition = getMaxPosition(descItem);

        if (descItem.getPosition() == null || (descItem.getPosition() > maxPosition)) {
            descItem.setPosition(maxPosition + 1);
        }

        // načtení hodnot, které je potřeba přesunout níž
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getDescItemType(),
                descItem.getNode(),
                descItem.getPosition() - 1);

        for (ArrDescItem descItemMove : descItems) {

            descItemMove.setDeleteChange(change);
            descItemRepository.save(descItemMove);

            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(descItemMove, descItemNew);
            descItemNew.setDescItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(change);
            descItemNew.setPosition(descItemMove.getPosition() + 1);

            descItemRepository.save(descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }

        descItem.setCreateChange(change);
        return descItemFactory.saveDescItemWithData(descItem, true);
    }

    /**
     * Kontrola typu a specifikace.
     *
     * @param descItem hodnota atributu
     */
    private void checkValidTypeAndSpec(final ArrDescItem descItem) {
        RulDescItemType descItemType = descItem.getDescItemType();
        RulDescItemSpec descItemSpec = descItem.getDescItemSpec();

        Assert.notNull(descItemType, "Hodnota atributu musí mít vyplněný typ");

        if (descItemType.getUseSpecification()) {
            Assert.notNull(descItemSpec, "Pro typ atributu je specifikace povinná");
        }

        if (descItemSpec != null) {
            List<RulDescItemSpec> descItemSpecs = descItemSpecRepository.findByDescItemType(descItemType);
            if (!descItemSpecs.contains(descItemSpec)) {
                throw new IllegalStateException("Specifikace neodpovídá typu hodnoty atributu");
            }
        }
    }

    /**
     * Smaže hodnotu atributu.
     *
     * @param descItem hodnota atributu
     * @param version  verze archivní pomůcky
     * @param change   změna operace
     * @param moveAfter posunout hodnoty po?
     * @return smazaná hodnota atributu
     */
    public ArrDescItem deleteDescriptionItem(final ArrDescItem descItem,
                                             final ArrFindingAidVersion version,
                                             final ArrChange change,
                                             final boolean moveAfter) {
        Assert.notNull(descItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro mazání musí být verze otevřená
        checkFindingAidVersionLock(version);

        if (moveAfter) {
            // načtení hodnot, které je potřeba přesunout výš
            List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                    descItem.getDescItemType(),
                    descItem.getNode(),
                    descItem.getPosition());

            copyDescItemsWithData(change, descItems, -1, version);
        }

        descItem.setDeleteChange(change);

        // sockety
        publishChangeDescItem(version, descItem);

        return descItemRepository.save(descItem);
    }

    /**
     * Provede posun (a odverzování) hodnot atributů s daty o požadovaný počet.
     *
     * @param change    změna operace
     * @param descItems seznam posunovaných hodnot atributu
     * @param diff      počet a směr posunu
     */
    private void copyDescItemsWithData(final ArrChange change, final List<ArrDescItem> descItems, final Integer diff,
                                       final ArrFindingAidVersion version) {
        for (ArrDescItem descItemMove : descItems) {

            descItemMove.setDeleteChange(change);
            descItemRepository.save(descItemMove);

            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(descItemMove, descItemNew);
            descItemNew.setDescItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(change);
            descItemNew.setPosition(descItemMove.getPosition() + diff);

            descItemRepository.save(descItemNew);

            // sockety
            publishChangeDescItem(version, descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }
    }

    /**
     * Vytvoří kopii seznamu atributů. Kopírovaný atribut patří zvolenému uzlu.
     *
     * @param node            uzel, který dostane kopírované atributy
     * @param sourceDescItems zdrojové atributy ke zkopírování
     * @param createChange    čas vytvoření nové kopie
     */
    public void copyDescItemWithDataToNode(final ArrNode node,
                                           final List<ArrDescItem> sourceDescItems,
                                           final ArrChange createChange,
                                           final ArrFindingAidVersion version) {
        for (ArrDescItem sourceDescItem : sourceDescItems) {
            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(sourceDescItem, descItemNew);
            descItemNew.setNode(node);
            descItemNew.setDescItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(createChange);
            descItemNew.setPosition(sourceDescItem.getPosition());
            descItemNew.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

            descItemRepository.save(descItemNew);

            // sockety
            publishChangeDescItem(version, descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(sourceDescItem, descItemNew);
        }
    }

    /**
     * Vypropagovani zmeny hodnoty atributu - sockety.
     *
     * @param version   verze archivní pomůcky
     * @param descItem  hodnota atributu
     */
    private void publishChangeDescItem(final ArrFindingAidVersion version, final ArrDescItem descItem) {
        notificationService.publishEvent(
                new EventChangeDescItem(EventType.DESC_ITEM_CHANGE, version.getFindingAidVersionId(),
                        descItem.getDescItemObjectId(), descItem.getNode().getNodeId()));
    }

    /**
     * Provede kopii dat mezi hodnotama atributů.
     *
     * @param descItemFrom z hodnoty atributu
     * @param descItemTo   do hodnoty atributu
     */
    private void copyDescItemData(final ArrDescItem descItemFrom, final ArrDescItem descItemTo) {
        List<ArrData> dataList = dataRepository.findByDescItem(descItemFrom);

        if (dataList.size() != 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        }

        ArrData data = dataList.get(0);

        try {
            ArrData dataNew = data.getClass().getConstructor().newInstance();

            BeanUtils.copyProperties(data, dataNew);
            dataNew.setDataId(null);
            dataNew.setDescItem(descItemTo);

            dataRepository.save(dataNew);
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem              hodnota atributu (změny)
     * @param nodeVersion           verze uzlu (optimistické zámky)
     * @param findingAidVersionId   identifikátor verze archivní pomůcky
     * @param createNewVersion      vytvořit novou verzi?
     * @return  upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeVersion,
                                             final Integer findingAidVersionId,
                                             final Boolean createNewVersion) {
        Assert.notNull(descItem);
        Assert.notNull(descItem.getPosition());
        Assert.notNull(descItem.getDescItemObjectId());
        Assert.notNull(nodeVersion);
        Assert.notNull(findingAidVersionId);
        Assert.notNull(createNewVersion);

        ArrChange change = null;
        ArrFindingAidVersion findingAidVersion = findingAidVersionRepository.findOne(findingAidVersionId);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

        if (descItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (descItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrDescItem descItemDB = descItems.get(0);

        ArrNode node = descItemDB.getNode();
        Assert.notNull(node);

        if (createNewVersion) {
            node.setVersion(nodeVersion);

            // uložení uzlu (kontrola optimistických zámků)
            saveNode(node);

            // vytvoření změny
            change = arrangementService.createChange();

            // uložení poslední uživatelské změny nad AP k verzi AP
            arrangementService.saveLastChangeFaVersion(change, findingAidVersion);
        }

        ArrDescItem descItemUpdated = updateDescriptionItemWithData(descItem, descItemDB, findingAidVersion, change, createNewVersion);

        // validace uzlu
        ruleService.conformityInfo(findingAidVersionId, Arrays.asList(descItemUpdated.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, Arrays.asList(descItemUpdated), null);

        return descItemUpdated;
    }


    /**
     * Najde scénář podle názvu.
     *
     * @param scenarionName  název scénáře
     * @param level          uzel, pro který hledáme
     * @param directionLevel směr přidání nového uzlu
     * @param version        verze stromu
     * @return scénář uzlu s daným názvem
     */
    public ScenarioOfNewLevel getDescriptionItamsOfScenario(final String scenarionName, final ArrLevel level,
                                                            final DirectionLevel directionLevel,
                                                            final ArrFindingAidVersion version) {
        Assert.notNull(scenarionName);
        Assert.notNull(level);
        Assert.notNull(directionLevel);
        Assert.notNull(version);

        List<ScenarioOfNewLevel> scenarioOfNewLevels = getDescriptionItemTypesForNewLevel(level, directionLevel,
                version);

        for (ScenarioOfNewLevel scenarioOfNewLevel : scenarioOfNewLevels) {
            if (scenarioOfNewLevel.getName().equals(scenarionName)) {
                return scenarioOfNewLevel;
            }
        }

        throw new IllegalArgumentException("Nebyl nalezen scénář s názvem " + scenarionName);
    }

    /**
     * Informace o možných scénářích založení nového uzlu
     *
     * @param level          založený uzel
     * @param directionLevel typ vladani
     * @param version        verze stromu
     * @return seznam možných scénařů
     */
    public List<ScenarioOfNewLevel> getDescriptionItemTypesForNewLevel(final ArrLevel level,
                                                                       final DirectionLevel directionLevel,
                                                                       final ArrFindingAidVersion version
    ) {
        Assert.notNull(version);
        Assert.notNull(level);

        return rulesExecutor.executeScenarioOfNewLevelRules(level, directionLevel, version);
    }

    /**
     * Informace o možných scénářích založení nového uzlu
     * @param nodeId            id uzlu
     * @param directionLevel    typ vladani
     * @param faVersionId       id verze
     * @return seznam možných scénařů
     */
    public List<ScenarioOfNewLevel> getDescriptionItemTypesForNewLevel(final Integer nodeId, final DirectionLevel directionLevel, final Integer faVersionId) {

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);
        ArrNode node = nodeRepository.findOne(nodeId);
        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootLevel().getNode(),
                version.getLockChange());

        return getDescriptionItemTypesForNewLevel(level, directionLevel, version);
    }

    /**
     * Upravení hodnoty atributu.
     *  - se spuštěním validace uzlu
     *
     * @param descItem              hodnota atributu (změny)
     * @param findingAidVersion     verze archivní pomůcky
     * @param change                změna
     * @param createNewVersion      vytvořit novou verzi?
     * @return  upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItem(final ArrDescItem descItem,
                                             final ArrFindingAidVersion findingAidVersion,
                                             final ArrChange change,
                                             final boolean createNewVersion) {
        Assert.notNull(descItem);
        Assert.notNull(descItem.getPosition());
        Assert.notNull(descItem.getDescItemObjectId());
        Assert.notNull(findingAidVersion);
        Assert.notNull(change);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

        if (descItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (descItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrDescItem descItemDB = descItems.get(0);

        ArrDescItem descItemUpdated = updateDescriptionItemWithData(descItem, descItemDB, findingAidVersion, change, createNewVersion);

        // validace uzlu
        ruleService.conformityInfo(findingAidVersion.getFindingAidVersionId(), Arrays.asList(descItemUpdated.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, Arrays.asList(descItemUpdated), null);

        return descItemUpdated;
    }

    /**
     * Upravení hodnoty atributu s daty.
     *
     * @param descItem          hodnota atributu (změny)
     * @param descItemDB        hodnota atributu - původní (pokud je null, donačte se)
     * @param version           verze archivní pomůcky
     * @param change            změna operace
     * @param createNewVersion  vytvořit novou verzi?
     * @return  upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItemWithData(final ArrDescItem descItem,
                                                      final ArrDescItem descItemDB,
                                                      final ArrFindingAidVersion version,
                                                      final ArrChange change,
                                                      final Boolean createNewVersion) {

        if (createNewVersion ^ change != null) {
            throw new IllegalArgumentException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        if (createNewVersion && version.getLockChange() != null) {
            throw new IllegalArgumentException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }

        ArrDescItem descItemOrig;
        if (descItemDB == null) {
            List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

            if (descItems.size() > 1) {
                throw new IllegalStateException("Hodnota musí být právě jedna");
            } else if (descItems.size() == 0) {
                throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
            }

            descItemOrig = descItems.get(0);
        } else {
            descItemOrig = descItemDB;
        }

        descItemOrig = descItemFactory.getDescItem(descItemOrig, null);
        ArrDescItem descItemUpdated;

        if (createNewVersion) {

            Integer positionOrig = descItemOrig.getPosition();
            Integer positionNew = descItem.getPosition();

            // změnila pozice, budou se provádět posuny
            if (positionOrig != positionNew) {

                int maxPosition = getMaxPosition(descItemOrig);

                if (descItem.getPosition() == null || (descItem.getPosition() > maxPosition)) {
                    descItem.setPosition(maxPosition + 1);
                }

                List<ArrDescItem> descItemsMove;
                Integer diff;

                if (positionNew < positionOrig) {
                    diff = 1;
                    descItemsMove = findDescItemsBetweenPosition(descItemOrig, positionNew, positionOrig - 1);
                } else {
                    diff = -1;
                    descItemsMove = findDescItemsBetweenPosition(descItemOrig, positionOrig + 1, positionNew);
                }

                copyDescItemsWithData(change, descItemsMove, diff, version);

            }

            try {
                ArrDescItem descItemNew = descItemOrig.getClass().newInstance();
                BeanUtils.copyProperties(descItemOrig, descItemNew);
                copyPropertiesSubclass(descItem, descItemNew, ArrDescItem.class);
                descItemNew.setDescItemSpec(descItem.getDescItemSpec());

                descItemOrig.setDeleteChange(change);
                descItemNew.setDescItemId(null);
                descItemNew.setCreateChange(change);
                descItemNew.setPosition(positionNew);

                descItemFactory.saveDescItem(descItemOrig);
                descItemUpdated = descItemFactory.saveDescItemWithData(descItemNew, true);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            copyPropertiesSubclass(descItem, descItemOrig, ArrDescItem.class);
            descItemOrig.setDescItemSpec(descItem.getDescItemSpec());
            descItemUpdated = descItemFactory.saveDescItemWithData(descItemOrig, false);
        }

        // sockety
        publishChangeDescItem(version, descItemUpdated);

        return descItemUpdated;
    }

    /**
     * Vyhledá maximální pozici v hodnotách atributu podle typu.
     *
     * @param descItem  hodnota atributu
     * @return  maximální pozice (počet položek)
     */
    private int getMaxPosition(final ArrDescItem descItem) {
        int maxPosition = 0;
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getDescItemType(),
                descItem.getNode(),
                0);
        for (ArrDescItem item : descItems) {
            if (item.getPosition() > maxPosition) {
                maxPosition = item.getPosition();
            }
        }
        return maxPosition;
    }

    /**
     * Vyhledá všechny hodnoty atributu mezi pozicemi.
     *
     * @param descItem      hodnota atributu
     * @param positionFrom  od pozice (včetně)
     * @param positionTo    do pozice (včetně)
     * @return  seznam nalezených hodnot atributů
     */
    private List<ArrDescItem> findDescItemsBetweenPosition(final ArrDescItem descItem,
                                                           final Integer positionFrom,
                                                           final Integer positionTo) {

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsBetweenPositions(descItem.getDescItemType(),
                descItem.getNode(), positionFrom, positionTo);

        return descItems;
    }

    /**
     * Kopíruje všechny property krom propert, které má zadaná třída.
     *
     * @param from z objektu
     * @param to   do objektu
     * @param aClass ignorovaná třída (subclass)
     * @param <T>    ignorovaná třída (subclass)
     * @param <TYPE> kopírovaná třída
     */
    private <T, TYPE extends T> void copyPropertiesSubclass(final TYPE from, final TYPE to, final Class<T> aClass) {
        String[] ignoreProperties;
        PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(aClass);
        ignoreProperties = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            ignoreProperties[i] = descriptors[i].getName();
        }

        BeanUtils.copyProperties(from, to, ignoreProperties);
    }
}
