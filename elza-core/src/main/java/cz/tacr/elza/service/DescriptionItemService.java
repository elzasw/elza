package cz.tacr.elza.service;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.api.vo.NodeTypeOperation;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.repository.DataPacketRefRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
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
    private FundVersionRepository fundVersionRepository;

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

    @Autowired
    private DataPartyRefRepository dataPartyRefRepository;
    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;
    @Autowired
    private DataPacketRefRepository dataPacketRefRepository;

    /**
     * Kontrola otevřené verze.
     *
     * @param fundVersion verze
     */
    private void checkFundVersionLock(final ArrFundVersion fundVersion) {
        if (fundVersion.getLockChange() != null) {
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
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return smazaná hodnota atributu
     */
    public ArrDescItem deleteDescriptionItem(final Integer descItemObjectId,
                                             final Integer nodeVersion,
                                             final Integer fundVersionId) {
        Assert.notNull(descItemObjectId);
        Assert.notNull(nodeVersion);
        Assert.notNull(fundVersionId);

        ArrChange change = arrangementService.createChange();
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
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

        ArrDescItem descItemDeleted = deleteDescriptionItem(descItem, fundVersion, change, true);

        // validace uzlu
        ruleService.conformityInfo(fundVersionId, Arrays.asList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, null, Arrays.asList(descItem));

        return descItemDeleted;
    }


    /**
     * Smaže hodnoty atributu podle typu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param fundVersionId   identifikátor verze archivní pomůcky
     * @param nodeId                identifikátor uzlu
     * @param nodeVersion           verze uzlu (optimistické zámky)
     * @param descItemTypeId        identifikátor typu hodnoty atributu
     * @return  upravený uzel
     */
    public ArrNode deleteDescriptionItemsByType(final Integer fundVersionId,
                                                final Integer nodeId,
                                                final Integer nodeVersion,
                                                final Integer descItemTypeId) {

        ArrChange change = arrangementService.createChange();
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
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
            descItemsDeleted.add(deleteDescriptionItem(descItem, fundVersion, change, false));
        }

        // validace uzlu
        ruleService.conformityInfo(fundVersionId, Arrays.asList(node.getNodeId()),
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
     * @param fundVersionId   identifikátor verze archivní pomůcky
     * @return vytvořená hodnota atributu
     */
    public ArrDescItem createDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeId,
                                             final Integer nodeVersion,
                                             final Integer fundVersionId) {
        Assert.notNull(descItem);
        Assert.notNull(nodeId);
        Assert.notNull(nodeVersion);
        Assert.notNull(fundVersionId);

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

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
                                             final ArrFundVersion version,
                                             @Nullable final ArrChange createChange) {

        ArrChange change = createChange == null ? arrangementService.createChange() : createChange;

        descItem.setNode(node);
        descItem.setCreateChange(change);
        descItem.setDeleteChange(null);
        descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        ArrDescItem descItemCreated = createDescriptionItemWithData(descItem, version, change);

        // validace uzlu
        ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(descItem.getNode().getNodeId()),
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
                                                     final ArrFundVersion version,
                                                     final ArrChange change) {
        Assert.notNull(descItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro vytváření musí být verze otevřená
        checkFundVersionLock(version);

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
            Assert.notNull(descItemSpec, "Pro typ atributu je nutné specifikaci vyplnit");
        } else {
            Assert.isNull(descItemSpec, "Pro typ atributu nesmí být specifikace vyplněná");
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
                                             final ArrFundVersion version,
                                             final ArrChange change,
                                             final boolean moveAfter) {
        Assert.notNull(descItem);
        Assert.notNull(version);
        Assert.notNull(change);

        // pro mazání musí být verze otevřená
        checkFundVersionLock(version);

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
                                       final ArrFundVersion version) {
        for (ArrDescItem descItemMove : descItems) {

            ArrDescItem descItemNew  = copyDescItem(change, descItemMove, descItemMove.getPosition() + diff);

            // sockety
            publishChangeDescItem(version, descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }
    }

    /**
     * Vytvoří kopii descItem. Původní hodnotu uzavře a vytvoří novou se stejnými daty (odverzování)
     * @param change změna, se kterou dojde k uzamčení a vytvoření kopie
     * @param descItem hodnota ke zkopírování
     * @param position pozice atributu
     * @return kopie atributu4
     */
    private ArrDescItem copyDescItem(final ArrChange change, final ArrDescItem descItem, final int position){
        descItem.setDeleteChange(change);
        descItemRepository.save(descItem);

        ArrDescItem descItemNew = new ArrDescItem();

        BeanUtils.copyProperties(descItem, descItemNew);
        descItemNew.setDescItemId(null);
        descItemNew.setDeleteChange(null);
        descItemNew.setCreateChange(change);
        descItemNew.setPosition(position);

        return descItemRepository.save(descItemNew);
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
                                           final ArrFundVersion version) {
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
    private void publishChangeDescItem(final ArrFundVersion version, final ArrDescItem descItem) {
        notificationService.publishEvent(
                new EventChangeDescItem(EventType.DESC_ITEM_CHANGE, version.getFundVersionId(),
                        descItem.getDescItemObjectId(), descItem.getNode().getNodeId(), descItem.getNode().getVersion()));
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
        ArrData dataNew = createCopyDescItemData(data, descItemTo);

        dataRepository.save(dataNew);
    }

    /**
     * Vytvoří kopii dat atributu.
     * @param data data atributu
     * @param newDescItem atribut, do kterého patří data
     * @return vytvořená kopie dat atributu (neuložená)
     */
    private ArrData createCopyDescItemData(final ArrData data, final ArrDescItem newDescItem) {
        try {
            ArrData dataNew = data.getClass().getConstructor().newInstance();

            BeanUtils.copyProperties(data, dataNew);
            dataNew.setDataId(null);
            dataNew.setDescItem(newDescItem);
            return dataNew;
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem              hodnota atributu (změny)
     * @param nodeVersion           verze uzlu (optimistické zámky)
     * @param fundVersionId   identifikátor verze archivní pomůcky
     * @param createNewVersion      vytvořit novou verzi?
     * @return  upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeVersion,
                                             final Integer fundVersionId,
                                             final Boolean createNewVersion) {
        Assert.notNull(descItem);
        Assert.notNull(descItem.getPosition());
        Assert.notNull(descItem.getDescItemObjectId());
        Assert.notNull(nodeVersion);
        Assert.notNull(fundVersionId);
        Assert.notNull(createNewVersion);

        ArrChange change = null;
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

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
        }

        ArrDescItem descItemUpdated = updateDescriptionItemWithData(descItem, descItemDB, fundVersion, change, createNewVersion);

        // validace uzlu
        ruleService.conformityInfo(fundVersionId, Arrays.asList(descItemUpdated.getNode().getNodeId()),
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
                                                            final ArrFundVersion version) {
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
                                                                       final ArrFundVersion version
    ) {
        Assert.notNull(version);
        Assert.notNull(level);

        return rulesExecutor.executeScenarioOfNewLevelRules(level, directionLevel, version);
    }

    /**
     * Informace o možných scénářích založení nového uzlu
     * @param nodeId            id uzlu
     * @param directionLevel    typ vladani
     * @param fundVersionId       id verze
     * @return seznam možných scénařů
     */
    public List<ScenarioOfNewLevel> getDescriptionItemTypesForNewLevel(final Integer nodeId, final DirectionLevel directionLevel, final Integer fundVersionId) {

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
        ArrNode node = nodeRepository.findOne(nodeId);
        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootNode(),
                version.getLockChange());

        return getDescriptionItemTypesForNewLevel(level, directionLevel, version);
    }

    /**
     * Upravení hodnoty atributu.
     *  - se spuštěním validace uzlu
     *
     * @param descItem              hodnota atributu (změny)
     * @param fundVersion     verze archivní pomůcky
     * @param change                změna
     * @param createNewVersion      vytvořit novou verzi?
     * @return  upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItem(final ArrDescItem descItem,
                                             final ArrFundVersion fundVersion,
                                             final ArrChange change,
                                             final boolean createNewVersion) {
        Assert.notNull(descItem);
        Assert.notNull(descItem.getPosition());
        Assert.notNull(descItem.getDescItemObjectId());
        Assert.notNull(fundVersion);
        Assert.notNull(change);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

        if (descItems.size() > 1) {
            throw new IllegalStateException("Hodnota musí být právě jedna");
        } else if (descItems.size() == 0) {
            throw new IllegalStateException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrDescItem descItemDB = descItems.get(0);

        ArrDescItem descItemUpdated = updateDescriptionItemWithData(descItem, descItemDB, fundVersion, change, createNewVersion);

        // validace uzlu
        ruleService.conformityInfo(fundVersion.getFundVersionId(), Arrays.asList(descItemUpdated.getNode().getNodeId()),
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
                                                      final ArrFundVersion version,
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


    public Map<Integer, Map<String, TitleValue>> createNodeValuesMap(final Set<Integer> subtreeNodeIds,
                                                                     @Nullable final TreeNode subtreeRoot,
                                                                     Set<RulDescItemType> descItemTypes,
                                                                     ArrFundVersion version) {
        Map<Integer, Map<String, TitleValue>> valueMap = new HashMap<>();

        if (descItemTypes.isEmpty()) {
            return valueMap;
        }

        //chceme nalézt atributy i pro rodiče podstromu
        Set<Integer> nodeIds = new HashSet<>(subtreeNodeIds);
        TreeNode rootParent = subtreeRoot;
        while(rootParent != null){
            nodeIds.add(rootParent.getId());
            rootParent = rootParent.getParent();
        }


        List<ArrData> dataList = dataRepository.findDescItemsByNodeIds(nodeIds, descItemTypes, version);
        Set<Integer> partyRefDataIds = new HashSet<>();
        Set<Integer> recordRefDataIds = new HashSet<>();
        Set<Integer> packetRefDataIds = new HashSet<>();
        Set<Integer> enumDataIds = new HashSet<>();

        for (ArrData data : dataList) {
            if (data.getDescItem().getPosition() > 1) {
                continue; // Používáme jen první hodnotu
            }
            String value = null;
            String code = data.getDescItem().getDescItemType().getCode();
            String specCode = data.getDescItem().getDescItemSpec() == null ? null : data.getDescItem().getDescItemSpec()
                    .getCode();
            Integer nodeId = data.getDescItem().getNode().getNodeId();
            if (data.getDataType().getCode().equals("ENUM")) {
                enumDataIds.add(data.getDataId());
            } else if (data.getDataType().getCode().equals("PARTY_REF")) {
                partyRefDataIds.add(data.getDataId());
            } else if (data.getDataType().getCode().equals("RECORD_REF")) {
                recordRefDataIds.add(data.getDataId());
            } else if (data.getDataType().getCode().equals("PACKET_REF")) {
                packetRefDataIds.add(data.getDataId());
            } else if (data.getDataType().getCode().equals("UNITDATE")) {
                ArrDataUnitdate unitDate = (ArrDataUnitdate) data;

                ParUnitdate parUnitdate = new ParUnitdate();
                parUnitdate.setCalendarType(unitDate.getCalendarType());
                parUnitdate.setFormat(unitDate.getFormat());
                parUnitdate.setValueFrom(unitDate.getValueFrom());
                parUnitdate.setValueFromEstimated(unitDate.getValueFromEstimated());
                parUnitdate.setValueTo(unitDate.getValueTo());
                parUnitdate.setValueToEstimated(unitDate.getValueToEstimated());

                value = UnitDateConvertor.convertToString(parUnitdate);
            } else if (data.getDataType().getCode().equals("STRING")) {
                ArrDataString stringtData = (ArrDataString) data;
                value = stringtData.getValue();
            } else if (data.getDataType().getCode().equals("TEXT") || data.getDataType().getCode().equals("FORMATTED_TEXT")) {
                ArrDataText textData = (ArrDataText) data;
                value = textData.getValue();
            } else if (data.getDataType().getCode().equals("UNITID")) {
                ArrDataUnitid unitId = (ArrDataUnitid) data;
                value = unitId.getValue();
            } else if (data.getDataType().getCode().equals("COORDINATES")) {
                ArrDataCoordinates coordinates = (ArrDataCoordinates) data;
                value = coordinates.getValue();
            } else if (data.getDataType().getCode().equals("INT")) {
                ArrDataInteger intData = (ArrDataInteger) data;
                value = intData.getValue().toString();
            } else if (data.getDataType().getCode().equals("DECIMAL")) {
                ArrDataDecimal decimalData = (ArrDataDecimal) data;
                value = decimalData.getValue().toPlainString();
            }

            String iconValue = getIconValue(data);

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue);
        }

        List<ArrData> enumData = dataRepository.findByDataIdsAndVersionFetchSpecification(enumDataIds, descItemTypes, version);
        for (ArrData data : enumData) {
            String value = data.getDescItem().getDescItemSpec().getName();
            String iconValue = getIconValue(data);
            String code = data.getDescItem().getDescItemType().getCode();
            String specCode = data.getDescItem().getDescItemSpec() == null ? null : data.getDescItem().getDescItemSpec()
                    .getCode();
            Integer nodeId = data.getDescItem().getNode().getNodeId();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue);
        }

        List<ArrDataPartyRef> partyData = dataPartyRefRepository.findByDataIdsAndVersionFetchPartyRecord(partyRefDataIds, descItemTypes, version);
        for (ArrDataPartyRef data : partyData) {
            String value = data.getParty().getRecord().getRecord();
            String iconValue = getIconValue(data);
            String code = data.getDescItem().getDescItemType().getCode();
            String specCode = data.getDescItem().getDescItemSpec() == null ? null : data.getDescItem().getDescItemSpec()
                    .getCode();
            Integer nodeId = data.getDescItem().getNode().getNodeId();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue);
        }

        List<ArrDataRecordRef> recordData = dataRecordRefRepository.findByDataIdsAndVersionFetchRecord(recordRefDataIds, descItemTypes, version);
        for (ArrDataRecordRef data : recordData) {
            String value = data.getRecord().getRecord();
            String iconValue = getIconValue(data);
            String code = data.getDescItem().getDescItemType().getCode();
            String specCode = data.getDescItem().getDescItemSpec() == null ? null : data.getDescItem().getDescItemSpec()
                    .getCode();
            Integer nodeId = data.getDescItem().getNode().getNodeId();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue);
        }

        List<ArrDataPacketRef> packetData = dataPacketRefRepository.findByDataIdsAndVersionFetchPacket(packetRefDataIds, descItemTypes, version);
        for (ArrDataPacketRef data : packetData) {
            ArrPacket packet = data.getPacket();
            RulPacketType packetType = packet.getPacketType();
            String value;
            if (packetType == null) {
                value = packet.getStorageNumber();
            } else {
                value = packetType.getName() + " " + packet.getStorageNumber();
            }
            String iconValue = getIconValue(data);
            String code = data.getDescItem().getDescItemType().getCode();
            String specCode = data.getDescItem().getDescItemSpec() == null ? null : data.getDescItem().getDescItemSpec()
                    .getCode();
            Integer nodeId = data.getDescItem().getNode().getNodeId();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue);
        }

        return valueMap;
    }

    private void addValuesToMap(Map<Integer, Map<String, TitleValue>> valueMap, String value, String code,
                                String specCode, Integer nodeId, String iconValue) {
        if (value == null && iconValue == null) {
            return;
        }

        Map<String, TitleValue> descItemCodeToValueMap = valueMap.get(nodeId);
        if (descItemCodeToValueMap == null) {
            descItemCodeToValueMap = new HashMap<>();
            valueMap.put(nodeId, descItemCodeToValueMap);
        }

        TitleValue titleValue = descItemCodeToValueMap.get(code);
        if (titleValue == null) {
            titleValue = new TitleValue();
            titleValue.setIconValue(iconValue);
            titleValue.setValue(value);
            titleValue.setSpecCode(specCode);
            descItemCodeToValueMap.put(code, titleValue);
        } else {
            if (titleValue.getValue() == null ) {
                titleValue.setValue(value);
            }

            if (titleValue.getIconValue() == null) {
                titleValue.setValue(value);
            }
        }
    }



    private String getIconValue(ArrData data) {
        if (data.getDescItem().getDescItemSpec() != null) {
            return data.getDescItem().getDescItemSpec().getCode();
        }
        return null;
    }


    /**
     * Nahrazení textu v hodnotách textových atributů.
     * @param version  verze stromu
     * @param descItemType typ atributu
     * @param nodes seznam uzlů, ve kterých hledáme
     * @param findText hledaný text v atributu
     * @param replaceText text, který nahradí hledaný text v celém textu
     */
    public void replaceDescItemValues(final ArrFundVersion version,
                                      final RulDescItemType descItemType,
                                      final Set<ArrNode> nodes,
                                      final String findText,
                                      final String replaceText) {
        Assert.notNull(version);
        Assert.notNull(descItemType);
        Assert.hasText(findText);
        Assert.hasText(replaceText);
        Assert.notEmpty(nodes);

        Map<Integer, ArrNode> nodesMap = ElzaTools.createEntityMap(nodes, n -> n.getNodeId());

        List<ArrData> dataToReplaceText = dataRepository.findByNodesContainingText(nodes, descItemType, findText);
        if(!dataToReplaceText.isEmpty()){


            ArrChange change = arrangementService.createChange();

            for (ArrData arrData : dataToReplaceText) {
                ArrNode clientNode = nodesMap.get(arrData.getDescItem().getNodeId());
                arrangementService.lockNode(arrData.getDescItem().getNode(), clientNode);

                replaceDescItemValue(arrData, findText, replaceText, change);

                publishChangeDescItem(version, arrData.getDescItem());
            }
        }
    }

    public Class<? extends ArrData> getDescItemDataTypeClass(final RulDescItemType descItemType) {
        switch (descItemType.getDataType().getCode()) {
            case "INT":
                return ArrDataInteger.class;
            case "STRING":
                return ArrDataString.class;
            case "TEXT":
            case "FORMATTED_TEXT":
                return ArrDataText.class;
            case "UNITDATE":
                return ArrDataUnitdate.class;
            case "UNITID":
                return ArrDataUnitdate.class;
            case "COORDINATES":
                return ArrDataCoordinates.class;
            case "PARTY_REF":
                return ArrDataPartyRef.class;
            case "RECORD_REF":
                return ArrDataRecordRef.class;
            case "DECIMAL":
                return ArrDataDecimal.class;
            case "PACKET_REF":
                return ArrDataPacketRef.class;
            case "ENUM":
                return ArrDataNull.class;
            default:
                throw new NotImplementedException("Nebyl namapován datový typ");
        }
    }

    /**
     * Provede nahrazení textu v hodnotě atributu.
     * @param data data atributu
     * @param searchString text, který hledáme
     * @param replaceString text, který nahradíme
     * @param change změna (odverzování)
     */
    private void replaceDescItemValue(final ArrData data, final String searchString, final String replaceString, final ArrChange change){


        ArrDescItem descItem = data.getDescItem();
        ArrDescItem newDescItem = copyDescItem(change, descItem, descItem.getPosition());

        ArrData newData = createCopyDescItemData(data, newDescItem);


        switch (data.getDescItem().getDescItemType().getDataType().getCode()) {
            case "STRING":
                ArrDataString oldStringData = (ArrDataString) data;

                ArrDataString newStringData = (ArrDataString) newData;
                newStringData.setValue(getReplacedDataValue(oldStringData.getValue(), searchString, replaceString));

                break;
            case "TEXT":
                ArrDataText oldTextData = (ArrDataText) data;
                ArrDataText newTextData = (ArrDataText) newData;
                newTextData.setValue(getReplacedDataValue(oldTextData.getValue(), searchString, replaceString));
                break;

            default:
                throw new IllegalStateException(
                        "Zatím není implementováno pro kod " + data.getDescItem().getDescItemType().getCode());
        }

        dataRepository.save(newData);
    }

    /**
     * Nahradí text v řetězci.
     *
     * @param text          text, ve kterém hledáme
     * @param searchString  text, který hledáme
     * @param replaceString text, který nahradíme
     * @return zpracovaný text
     */
    private String getReplacedDataValue(final String text, final String searchString, final String replaceString) {
        Assert.notNull(text);
        Assert.notNull(searchString);
        Assert.notNull(replaceString);

        return StringUtils.replace(text, searchString, replaceString);
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
