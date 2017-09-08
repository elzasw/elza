package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ItemSpecRegisterRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
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
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.CoordinatesTitleValue;
import cz.tacr.elza.domain.vo.JsonTableTitleValue;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;
import cz.tacr.elza.domain.vo.UnitdateTitleValue;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.repository.DataPacketRefRepository;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Description Item management
 *
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
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

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

    @Autowired
    private ItemService itemService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private ItemSpecRegisterRepository itemSpecRegisterRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    /**
     * Kontrola otevřené verze.
     *
     * @param fundVersion verze
     */
    private void checkFundVersionLock(final ArrFundVersion fundVersion) {
        if (fundVersion.getLockChange() != null) {
            throw new BusinessException("Nelze provést verzovanou změnu v uzavřené verzi.", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
    }

    /**
     * Uložení uzlu - optimistické zámky
     *
     * @param node uzel
     * @param change
     * @return uložený uzel
     */
    private ArrNode saveNode(final ArrNode node, final ArrChange change) {
        node.setLastUpdate(change.getChangeDate());
        nodeRepository.save(node);
        nodeRepository.flush();
        return node;
    }

    /**
     * Smaže hodnotu atributu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param descItemObjectId identifikátor hodnoty atributu
     * @param nodeVersion      verze uzlu (optimistické zámky)
     * @param fundVersionId    identifikátor verze archivní pomůcky
     * @return smazaná hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDescItem deleteDescriptionItem(final Integer descItemObjectId,
                                             final Integer nodeVersion,
                                             @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(descItemObjectId, "Nebyl vyplněn jednoznačný identifikátor descItem");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemObjectId);

        if (descItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (descItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }

        ArrDescItem descItem = descItems.get(0);
        descItem.getNode().setVersion(nodeVersion);
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_DESC_ITEM, descItem.getNode());

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(descItem.getNode(), change);

        ArrDescItem descItemDeleted = deleteDescriptionItem(descItem, fundVersion, change, true);
        arrangementCacheService.deleteDescItem(descItem.getNodeId(), descItemObjectId);

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
     * @param fundVersionId  identifikátor verze archivní pomůcky
     * @param nodeId         identifikátor uzlu
     * @param nodeVersion    verze uzlu (optimistické zámky)
     * @param descItemTypeId identifikátor typu hodnoty atributu
     * @return upravený uzel
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrNode deleteDescriptionItemsByType(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                final Integer nodeId,
                                                final Integer nodeVersion,
                                                final Integer descItemTypeId) {

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        ArrNode node = nodeRepository.findOne(nodeId);
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_DESC_ITEM, node);
        node.setVersion(nodeVersion);

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(node, change);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemType, node);

        if (descItems.size() == 0) {
            throw new SystemException("Nebyla nalezena žádná hodnota atributu ke smazání");
        }

        List<ArrDescItem> descItemsDeleted = new ArrayList<>(descItems.size());
        List<Integer> descItemObjectIds = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            ArrDescItem arrDescItem = deleteDescriptionItem(descItem, fundVersion, change, false);
            descItemObjectIds.add(arrDescItem.getDescItemObjectId());
            descItemsDeleted.add(arrDescItem);
        }

        arrangementCacheService.deleteDescItems(nodeId, descItemObjectIds);

        // validace uzlu
        ruleService.conformityInfo(fundVersionId, Arrays.asList(node.getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, null, descItemsDeleted);

        return node;
    }

    /**
     * Smaže hodnoty atributu podle typu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param fundVersionId  identifikátor verze archivní pomůcky
     * @param nodeId         identifikátor uzlu
     * @param nodeVersion    verze uzlu (optimistické zámky)
     * @param descItemTypeId identifikátor typu hodnoty atributu
     * @return upravený uzel
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrNode deleteDescriptionItemsByTypeWithoutVersion(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
                                                              final Integer nodeId,
                                                              final Integer nodeVersion,
                                                              final Integer descItemTypeId) {

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);

        Assert.notNull(fundVersion, "Verze archivní pomůcky neexistuje");
        Assert.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        ArrNode node = nodeRepository.findOne(nodeId);
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_DESC_ITEM, node);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemType, node);

        if (descItems.size() == 0) {
            throw new SystemException("Nebyla nalezena žádná hodnota atributu ke smazání");
        }

        List<ArrDescItem> descItemsDeleted = new ArrayList<>(descItems.size());
        List<Integer> descItemObjectIds = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
            ArrDescItem arrDescItem = deleteDescriptionItem(descItem, fundVersion, change, false);
            descItemObjectIds.add(arrDescItem.getDescItemObjectId());
            descItemsDeleted.add(arrDescItem);
        }

        arrangementCacheService.deleteDescItems(nodeId, descItemObjectIds);

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
     * @param descItem      hodnota atributu
     * @param nodeId        identifikátor uzlu
     * @param nodeVersion   verze uzlu (optimistické zámky)
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return vytvořená hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDescItem createDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeId,
                                             final Integer nodeVersion,
                                             @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node, "JP musí být vyplněna");

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_DESC_ITEM, node);

        // uložení uzlu (kontrola optimistických zámků)
        node.setVersion(nodeVersion);
        saveNode(node, change);

        return createDescriptionItem(descItem, node, version, change);
    }

    /**
     * Vytvoření hodnoty atributu.
     * - s kontrolou verze uzlu
     * - se spuštěním validace uzlu
     *
     * @param descItems     hodnota atributu
     * @param nodeId        identifikátor uzlu
     * @param nodeVersion   verze uzlu (optimistické zámky)
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return vytvořená hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public List<ArrDescItem> createDescriptionItems(final List<ArrDescItem> descItems,
                                                    final Integer nodeId,
                                                    final Integer nodeVersion,
                                                    @AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId) {
        Assert.notNull(descItems, "Hodnoty atributů musí být vyplněny");
        Assert.notEmpty(descItems, "Alespoň jedna hodnota atributu musí být vyplněna");
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        ArrNode node = nodeRepository.findOne(nodeId);
        Assert.notNull(node, "JP musí být vyplněna");

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_DESC_ITEM, node);

        // uložení uzlu (kontrola optimistických zámků)
        node.setVersion(nodeVersion);
        saveNode(node, change);

        return createDescriptionItems(descItems, node, version, change);
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

        ArrChange change = createChange == null ? arrangementService.createChange(ArrChange.Type.ADD_DESC_ITEM, node) : createChange;

        descItem.setNode(node);
        descItem.setCreateChange(change);
        descItem.setDeleteChange(null);
        descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        ArrDescItem descItemCreated = createDescriptionItemWithData(descItem, version, change);

        // validace uzlu
        ruleService.conformityInfo(version.getFundVersionId(), Collections.singletonList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, Collections.singletonList(descItem), null, null);

        // sockety
        publishChangeDescItem(version, descItemCreated);

        return descItemCreated;
    }

    /**
     * Vytvoření hodnoty atributu. Při ukládání nedojde ke zvýšení verze uzlu.
     * - se spuštěním validace uzlu
     *
     * @param descItems hodnota atributu
     * @param node      uzel, kterému přidáme hodnotu
     * @param version   verze stromu
     * @return vytvořená hodnota atributu
     */
    public List<ArrDescItem> createDescriptionItems(final List<ArrDescItem> descItems,
                                                    final ArrNode node,
                                                    final ArrFundVersion version,
                                                    @Nullable final ArrChange createChange) {

        ArrChange change = createChange == null ? arrangementService.createChange(ArrChange.Type.ADD_DESC_ITEM, node) : createChange;
        List<ArrDescItem> createdItems = new ArrayList<>();
        for (ArrDescItem descItem :
                descItems) {
            descItem.setNode(node);
            descItem.setCreateChange(change);
            descItem.setDeleteChange(null);
            descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

            ArrDescItem created = createDescriptionItemWithData(descItem, version, change);
            createdItems.add(created);

            // sockety
            publishChangeDescItem(version, created);
        }

        // validace uzlu
        ruleService.conformityInfo(version.getFundVersionId(), Collections.singletonList(node.getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, createdItems, null, null);

        return createdItems;
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
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

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
                descItem.getItemType(),
                descItem.getNode(),
                descItem.getPosition() - 1);

        List<ArrDescItem> descItemNews = new ArrayList<>(descItems.size());
        for (ArrDescItem descItemMove : descItems) {

            descItemMove.setDeleteChange(change);
            descItemRepository.save(descItemMove);

            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(descItemMove, descItemNew);
            descItemNew.setItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(change);
            descItemNew.setPosition(descItemMove.getPosition() + 1);

            descItemRepository.save(descItemNew);

            descItemNews.add(descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }

        if (CollectionUtils.isNotEmpty(descItemNews)) {
            arrangementCacheService.changeDescItems(descItemNews.get(0).getNodeId(), descItemNews, true);
        }

        descItem.setCreateChange(change);
        descItemFactory.saveDescItemWithData(descItem, true);

        arrangementCacheService.createDescItem(descItem.getNodeId(), descItem);
        return descItem;
    }

    /**
     * Kontrola typu a specifikace.
     *
     * @param descItem hodnota atributu
     */
    private void checkValidTypeAndSpec(final ArrDescItem descItem) {
        RulItemType descItemType = descItem.getItemType();
        RulItemSpec descItemSpec = descItem.getItemSpec();

        Assert.notNull(descItemType, "Hodnota atributu musí mít vyplněný typ");

        if (descItemType.getUseSpecification()) {
            if (descItemSpec == null) {
                throw new BusinessException("Pro typ atributu je nutné specifikaci vyplnit", ArrangementCode.ITEM_SPEC_NOT_FOUND).level(Level.WARNING);
            }

            if (descItemType.getDataType().getCode().equals("RECORD_REF")) {
                Set<Integer> registerTypeIds = itemSpecRegisterRepository.findIdsByItemSpecId(descItemSpec);
                Set<Integer> registerTypeIdTree = registerTypeRepository.findSubtreeIds(registerTypeIds);
                ArrDataRecordRef data = (ArrDataRecordRef) descItem.getData();
                if (!registerTypeIdTree.contains(data.getRecord().getRegisterTypeId())) {
                    throw new BusinessException("Hodnota neodpovídá typu rejstříku podle specifikace", RegistryCode.FOREIGN_ENTITY_INVALID_SUBTYPE).level(Level.WARNING);
                }
            }

        } else {
            if (descItemSpec != null) {
                throw new BusinessException("Pro typ atributu nesmí být specifikace vyplněná", ArrangementCode.ITEM_SPEC_FOUND).level(Level.WARNING);
            }
        }

        if (descItemSpec != null) {
            List<RulItemSpec> descItemSpecs = itemSpecRepository.findByItemType(descItemType);
            if (!descItemSpecs.contains(descItemSpec)) {
                throw new SystemException("Specifikace neodpovídá typu hodnoty atributu");
            }
        }
    }

    /**
     * Smaže hodnotu atributu.
     *
     * Funkce současně posílá notifikaci přes WS
     * @param descItem  hodnota atributu
     * @param version   verze archivní pomůcky
     * @param change    změna operace
     * @param moveAfter posunout hodnoty po?
     * @return smazaná hodnota atributu
     */
    public ArrDescItem deleteDescriptionItem(final ArrDescItem descItem,
                                             final ArrFundVersion version,
                                             final ArrChange change,
                                             final boolean moveAfter) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

        // pro mazání musí být verze otevřená
        checkFundVersionLock(version);

        if (moveAfter) {
            // načtení hodnot, které je potřeba přesunout výš
            List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                    descItem.getItemType(),
                    descItem.getNode(),
                    descItem.getPosition());

            copyDescItemsWithData(change, descItems, -1, version);
        }

        descItem.setDeleteChange(change);

        ArrDescItem retDescItem = descItemRepository.save(descItem);

        // sockety
        publishChangeDescItem(version, retDescItem);

        return retDescItem;
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
        List<ArrDescItem> descItemNews = new ArrayList<>(descItems.size());
        for (ArrDescItem descItemMove : descItems) {

            ArrDescItem descItemNew = copyDescItem(change, descItemMove, descItemMove.getPosition() + diff);

            // sockety
            publishChangeDescItem(version, descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(descItemMove, descItemNew);
        }

        if (CollectionUtils.isNotEmpty(descItemNews)) {
            arrangementCacheService.changeDescItems(descItemNews.get(0).getNodeId(), descItemNews, true);
        }
    }

    /**
     * Vytvoří kopii descItem. Původní hodnotu uzavře a vytvoří novou se stejnými daty (odverzování)
     *
     * @param change   změna, se kterou dojde k uzamčení a vytvoření kopie
     * @param descItem hodnota ke zkopírování
     * @param position pozice atributu
     * @return kopie atributu4
     */
    private ArrDescItem copyDescItem(final ArrChange change, final ArrDescItem descItem, final int position) {
        descItem.setDeleteChange(change);
        descItemRepository.save(descItem);

        ArrDescItem descItemNew;
        //try {
        descItemNew = new ArrDescItem();
        /*} catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }*/

        BeanUtils.copyProperties(descItem, descItemNew);
        descItemNew.setItemId(null);
        descItemNew.setDeleteChange(null);
        descItemNew.setCreateChange(change);
        descItemNew.setPosition(position);

        return descItemRepository.save(descItemNew);
    }

    /**
     * Vytvoří kopii seznamu atributů. Kopírovaný atribut patří zvolenému uzlu.
     *  @param node            uzel, který dostane kopírované atributy
     * @param sourceDescItems zdrojové atributy ke zkopírování
     * @param createChange    čas vytvoření nové kopie
     */
    public List<ArrDescItem> copyDescItemWithDataToNode(final ArrNode node,
                                                        final List<ArrDescItem> sourceDescItems,
                                                        final ArrChange createChange,
                                                        final ArrFundVersion version) {
        List<ArrDescItem> result = new ArrayList<>(sourceDescItems.size());
        for (ArrDescItem sourceDescItem : sourceDescItems) {
            ArrDescItem descItemNew = new ArrDescItem();

            BeanUtils.copyProperties(sourceDescItem, descItemNew);
            descItemNew.setNode(node);
            descItemNew.setItemId(null);
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(createChange);
            descItemNew.setPosition(sourceDescItem.getPosition());
            descItemNew.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

            descItemRepository.save(descItemNew);

            // sockety
            //publishChangeDescItem(version, descItemNew);

            // pro odverzovanou hodnotu atributu je nutné vytvořit kopii dat
            copyDescItemData(sourceDescItem, descItemNew);
            result.add(descItemNew);
        }
        if (CollectionUtils.isNotEmpty(result)) {
            arrangementCacheService.createDescItems(node.getNodeId(), result);
        }
        return result;
    }

    /**
     * Vypropagovani zmeny hodnoty atributu - sockety.
     *
     * @param version  verze archivní pomůcky
     * @param descItem hodnota atributu
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
        if (BooleanUtils.isNotTrue(descItemFrom.getData() == null)) {
            ArrData data = descItemFrom.getData();
            ArrData dataNew = createCopyDescItemData(data, descItemTo);
            dataRepository.save(dataNew);
        }
    }

    /**
     * Vytvoří kopii dat atributu.
     *
     * @param data        data atributu
     * @param newDescItem atribut, do kterého patří data
     * @return vytvořená kopie dat atributu (neuložená)
     */
    private ArrData createCopyDescItemData(final ArrData data, final ArrDescItem newDescItem) {
        try {
            ArrData dataNew = data.getClass().getConstructor().newInstance();

            BeanUtils.copyProperties(data, dataNew);
            dataNew.setDataId(null);
            newDescItem.setData(dataNew);
            return dataNew;
        } catch (Exception e) {
            throw new SystemException(e.getCause());
        }
    }

    /**
     * Upravení hodnoty atributu.
     *
     * @param descItem         hodnota atributu (změny)
     * @param nodeVersion      verze uzlu (optimistické zámky)
     * @param fundVersionId    identifikátor verze archivní pomůcky
     * @param createNewVersion vytvořit novou verzi?
     * @return upravená výsledná hodnota atributu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public ArrDescItem updateDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeVersion,
                                             final @AuthParam(type = AuthParam.Type.FUND_VERSION) Integer fundVersionId,
                                             final Boolean createNewVersion) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");
        Assert.notNull(descItem.getPosition(), "Pozice musí být vyplněna");
        Assert.notNull(descItem.getDescItemObjectId(), "Identifikátor hodnoty atributu musí být vyplněn");
        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
        Assert.notNull(createNewVersion, "Vytvořit novou verzi musí být vyplněno");

        ArrChange change = null;
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

        if (descItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (descItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
        }
        ArrDescItem descItemDB = descItems.get(0);

        ArrNode node = descItemDB.getNode();
        Assert.notNull(node, "JP musí být vyplněna");

        if (createNewVersion) {
            node.setVersion(nodeVersion);

            // vytvoření změny
            change = arrangementService.createChange(ArrChange.Type.UPDATE_DESC_ITEM, node);

            // uložení uzlu (kontrola optimistických zámků)
            saveNode(node, change);
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
        Assert.notNull(scenarionName, "Název scénáře musí být vyplněn");
        Assert.notNull(level, "Level musí být vyplněn");
        Assert.notNull(directionLevel, "Směr založení musí být vyplněn");
        Assert.notNull(version, "Verze AS musí být vyplněna");

        List<ScenarioOfNewLevel> scenarioOfNewLevels = getDescriptionItemTypesForNewLevel(level, directionLevel,
                version);

        for (ScenarioOfNewLevel scenarioOfNewLevel : scenarioOfNewLevels) {
            if (scenarioOfNewLevel.getName().equals(scenarionName)) {
                return scenarioOfNewLevel;
            }
        }

        throw new ObjectNotFoundException("Nebyl nalezen scénář s názvem " + scenarionName, BaseCode.ID_NOT_EXIST).setId(scenarionName);
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
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(level, "Level musí být vyplněn");

        return rulesExecutor.executeScenarioOfNewLevelRules(level, directionLevel, version);
    }

    /**
     * Informace o možných scénářích založení nového uzlu
     *
     * @param nodeId         id uzlu
     * @param directionLevel typ vladani
     * @param fundVersionId  id verze
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
     * - se spuštěním validace uzlu
     *
     * @param descItem         hodnota atributu (změny)
     * @param fundVersion      verze archivní pomůcky
     * @param change           změna
     * @param createNewVersion vytvořit novou verzi?
     * @return upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItem(final ArrDescItem descItem,
                                             final ArrFundVersion fundVersion,
                                             final ArrChange change,
                                             final boolean createNewVersion) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");
        Assert.notNull(descItem.getPosition(), "Pozice musí být vyplněna");
        Assert.notNull(descItem.getDescItemObjectId(), "Identifikátor hodnoty atributu musí být vyplněn");
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

        if (descItems.size() > 1) {
            throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
        } else if (descItems.size() == 0) {
            throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
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
     * @param descItem         hodnota atributu (změny)
     * @param descItemDB       hodnota atributu - původní (pokud je null, donačte se)
     * @param version          verze archivní pomůcky
     * @param change           změna operace
     * @param createNewVersion vytvořit novou verzi?
     * @return upravená výsledná hodnota atributu
     */
    public ArrDescItem updateDescriptionItemWithData(final ArrDescItem descItem,
                                                     final ArrDescItem descItemDB,
                                                     final ArrFundVersion version,
                                                     final ArrChange change,
                                                     final Boolean createNewVersion) {

        if (createNewVersion ^ change != null) {
            throw new SystemException("Pokud vytvářím novou verzi, musí být předaná reference změny. Pokud verzi nevytvářím, musí být reference změny null.");
        }

        if (createNewVersion && version.getLockChange() != null) {
            throw new SystemException("Nelze provést verzovanou změnu v uzavřené verzi.");
        }

        ArrDescItem descItemOrig;
        if (descItemDB == null) {
            List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItem.getDescItemObjectId());

            if (descItems.size() > 1) {
                throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
            } else if (descItems.size() == 0) {
                throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
            }

            descItemOrig = descItems.get(0);
        } else {
            descItemOrig = descItemDB;
        }

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
                ArrDescItem descItemNew = new ArrDescItem();
                BeanUtils.copyProperties(descItemOrig, descItemNew);
                itemService.copyPropertiesSubclass(descItem, descItemNew, ArrDescItem.class);
                descItemNew.setItemSpec(descItem.getItemSpec());

                descItemOrig.setDeleteChange(change);
                descItemNew.setItemId(null);
                descItemNew.setCreateChange(change);
                descItemNew.setPosition(positionNew);
                descItemNew.setData(descItem.getData());

                descItemFactory.saveDescItem(descItemOrig);
                descItemUpdated = descItemFactory.saveDescItemWithData(descItemNew, true);
            } catch (Exception e) {
                throw new SystemException(e);
            }
        } else {
            itemService.copyPropertiesSubclass(descItem, descItemOrig, ArrDescItem.class);
            descItemOrig.setItemSpec(descItem.getItemSpec());
            descItemUpdated = descItemFactory.saveDescItemWithData(descItemOrig, false);
        }

        checkValidTypeAndSpec(descItemUpdated);

        arrangementCacheService.changeDescItem(descItemUpdated.getNodeId(), descItemUpdated, false);

        // sockety
        publishChangeDescItem(version, descItemUpdated);

        return descItemUpdated;
    }


    public Map<Integer, Map<String, TitleValues>> createNodeValuesMap(final Set<Integer> subtreeNodeIds,
                                                                      @Nullable final TreeNode subtreeRoot,
                                                                      final Set<RulItemType> descItemTypes,
                                                                      final ArrFundVersion version) {
        Map<Integer, Map<String, TitleValues>> valueMap = new HashMap<>();

        if (descItemTypes.isEmpty()) {
            return valueMap;
        }

        //chceme nalézt atributy i pro rodiče podstromu
        Set<Integer> nodeIds = new HashSet<>(subtreeNodeIds);
        TreeNode rootParent = subtreeRoot;
        while (rootParent != null) {
            nodeIds.add(rootParent.getId());
            rootParent = rootParent.getParent();
        }

        List<ArrDescItem> descItemList = descItemRepository.findDescItemsByNodeIds(nodeIds, descItemTypes, version);

        for (ArrDescItem descItem : descItemList) {

            TitleValue value = null;
            String code = descItem.getItemType().getCode();
            String specCode = descItem.getItemSpec() == null ? null : descItem.getItemSpec()
                    .getCode();
            Integer nodeId = descItem.getNodeId();
            Integer position = descItem.getPosition();

            ArrData data = descItem.getData();

            if (data == null) { // undefined item
                value = new TitleValue(ArrangementService.UNDEFINED);
            } else if (data.getDataType().getCode().equals("ENUM")) {
                value = new TitleValue(descItem.getItemSpec().getName());
            } else if (data.getDataType().getCode().equals("PARTY_REF")) {
                ArrDataPartyRef partyData = (ArrDataPartyRef) data;
                value = new TitleValue(partyData.getParty().getRecord().getRecord());
            } else if (data.getDataType().getCode().equals("RECORD_REF")) {
                ArrDataRecordRef recordData = (ArrDataRecordRef) data;
                value = new TitleValue(recordData.getRecord().getRecord());
            } else if (data.getDataType().getCode().equals("PACKET_REF")) {
                ArrPacket packet = ((ArrDataPacketRef) data).getPacket();
                RulPacketType packetType = packet.getPacketType();
                if (packetType == null) {
                    value = new TitleValue(packet.getStorageNumber());
                } else {
                    value = new TitleValue(packetType.getName() + ": " + packet.getStorageNumber());
                }
            } else if (data.getDataType().getCode().equals("UNITDATE")) {
                ArrDataUnitdate unitDate = (ArrDataUnitdate) data;

                ParUnitdate parUnitdate = new ParUnitdate();
                parUnitdate.setCalendarType(unitDate.getCalendarType());
                parUnitdate.setFormat(unitDate.getFormat());
                parUnitdate.setValueFrom(unitDate.getValueFrom());
                parUnitdate.setValueFromEstimated(unitDate.getValueFromEstimated());
                parUnitdate.setValueTo(unitDate.getValueTo());
                parUnitdate.setValueToEstimated(unitDate.getValueToEstimated());

                value = new UnitdateTitleValue(UnitDateConvertor.convertToString(parUnitdate),
                        unitDate.getCalendarType().getCalendarTypeId());
            } else if (data.getDataType().getCode().equals("STRING")) {
                ArrDataString stringtData = (ArrDataString) data;
                value = new TitleValue(stringtData.getValue());
            } else if (data.getDataType().getCode().equals("TEXT") || data.getDataType().getCode().equals("FORMATTED_TEXT")) {
                ArrDataText textData = (ArrDataText) data;
                value = new TitleValue(textData.getValue());
            } else if (data.getDataType().getCode().equals("UNITID")) {
                ArrDataUnitid unitId = (ArrDataUnitid) data;
                value = new TitleValue(unitId.getValue());
            } else if (data.getDataType().getCode().equals("INT")) {
                ArrDataInteger intData = (ArrDataInteger) data;
                value = new TitleValue(intData.getValue().toString());
            } else if (data.getDataType().getCode().equals("DECIMAL")) {
                ArrDataDecimal decimalData = (ArrDataDecimal) data;
                value = new TitleValue(decimalData.getValue().toPlainString());
            } else if (data.getDataType().getCode().equals("COORDINATES")) {
                ArrDataCoordinates coordinates = (ArrDataCoordinates) data;
                value = new CoordinatesTitleValue(coordinates.getValue());
            } else if (data.getDataType().getCode().equals("JSON_TABLE")) {
                ArrDataJsonTable table = (ArrDataJsonTable) data;
                value = new JsonTableTitleValue(table.getFulltextValue(), table.getValue().getRows().size());
            }

            if (value != null) {
                String iconValue = getIconValue(descItem);
                addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue, position);
            }
        }

        return valueMap;
    }

    /**
     * Vytvoření mapy popisků JP.
     *
     * @param subtreeNodeIds seznam identifikátorů JP
     * @param descItemTypes  seznam typů atributů
     * @param changeId       identifikátor změny, vůči které sestavujeme popisky
     * @return mapa popisků
     */
    public Map<Integer, Map<String, TitleValues>> createNodeValuesMap(final Set<Integer> subtreeNodeIds,
                                                                      final Set<RulItemType> descItemTypes,
                                                                      final Integer changeId) {
        Map<Integer, Map<String, TitleValues>> valueMap = new HashMap<>();

        if (descItemTypes.isEmpty()) {
            return valueMap;
        }

        //chceme nalézt atributy i pro rodiče podstromu
        Set<Integer> nodeIds = new HashSet<>(subtreeNodeIds);

        List<ArrDescItem> descItemList = descItemRepository.findDescItemsByNodeIds(nodeIds, descItemTypes, changeId);

        for (ArrDescItem descItem : descItemList) {

            TitleValue value = null;
            String code = descItem.getItemType().getCode();
            String specCode = descItem.getItemSpec() == null ? null : descItem.getItemSpec().getCode();
            Integer nodeId = descItem.getNodeId();
            Integer position = descItem.getPosition();

            ArrData data = descItem.getData();

            if (data == null) {
                continue;
            }

            if (data.getDataType().getCode().equals("ENUM")) {
                value = new TitleValue(descItem.getItemSpec().getName());
            } else if (data.getDataType().getCode().equals("PARTY_REF")) {
                ArrDataPartyRef partyData = (ArrDataPartyRef) data;
                value = new TitleValue(partyData.getParty().getRecord().getRecord());
            } else if (data.getDataType().getCode().equals("RECORD_REF")) {
                ArrDataRecordRef recordData = (ArrDataRecordRef) data;
                value = new TitleValue(recordData.getRecord().getRecord());
            } else if (data.getDataType().getCode().equals("PACKET_REF")) {
                ArrPacket packet = ((ArrDataPacketRef) data).getPacket();
                RulPacketType packetType = packet.getPacketType();
                if (packetType == null) {
                    value = new TitleValue(packet.getStorageNumber());
                } else {
                    value = new TitleValue(packetType.getName() + ": " + packet.getStorageNumber());
                }
            } else if (data.getDataType().getCode().equals("UNITDATE")) {
                ArrDataUnitdate unitDate = (ArrDataUnitdate) data;

                ParUnitdate parUnitdate = new ParUnitdate();
                parUnitdate.setCalendarType(unitDate.getCalendarType());
                parUnitdate.setFormat(unitDate.getFormat());
                parUnitdate.setValueFrom(unitDate.getValueFrom());
                parUnitdate.setValueFromEstimated(unitDate.getValueFromEstimated());
                parUnitdate.setValueTo(unitDate.getValueTo());
                parUnitdate.setValueToEstimated(unitDate.getValueToEstimated());

                value = new UnitdateTitleValue(UnitDateConvertor.convertToString(parUnitdate),
                        unitDate.getCalendarType().getCalendarTypeId());
            } else if (data.getDataType().getCode().equals("STRING")) {
                ArrDataString stringtData = (ArrDataString) data;
                value = new TitleValue(stringtData.getValue());
            } else if (data.getDataType().getCode().equals("TEXT") || data.getDataType().getCode().equals("FORMATTED_TEXT")) {
                ArrDataText textData = (ArrDataText) data;
                value = new TitleValue(textData.getValue());
            } else if (data.getDataType().getCode().equals("UNITID")) {
                ArrDataUnitid unitId = (ArrDataUnitid) data;
                value = new TitleValue(unitId.getValue());
            } else if (data.getDataType().getCode().equals("INT")) {
                ArrDataInteger intData = (ArrDataInteger) data;
                value = new TitleValue(intData.getValue().toString());
            } else if (data.getDataType().getCode().equals("DECIMAL")) {
                ArrDataDecimal decimalData = (ArrDataDecimal) data;
                value = new TitleValue(decimalData.getValue().toPlainString());
            } else if (data.getDataType().getCode().equals("COORDINATES")) {
                ArrDataCoordinates coordinates = (ArrDataCoordinates) data;
                value = new CoordinatesTitleValue(coordinates.getValue());
            } else if (data.getDataType().getCode().equals("JSON_TABLE")) {
                ArrDataJsonTable table = (ArrDataJsonTable) data;
                value = new JsonTableTitleValue(table.getFulltextValue(), table.getValue().getRows().size());
            }

            if (value != null) {
                String iconValue = getIconValue(descItem);
                addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue, position);
            }
        }

        /*List<ArrData> enumData = dataRepository.findByDataIdsAndVersionFetchSpecification(enumDataIds, descItemTypes, changeId);
        for (ArrData data : enumData) {
            TitleValue value = new TitleValue(data.getItem().getItemSpec().getName());
            String iconValue = getIconValue(data);
            String code = data.getItem().getItemType().getCode();
            String specCode = data.getItem().getItemSpec() == null ? null : data.getItem().getItemSpec()
                    .getCode();
            Integer nodeId = data.getItem().getNodeId();
            Integer position = data.getItem().getPosition();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue, position);
        }

        List<ArrDataPartyRef> partyData = dataPartyRefRepository.findByDataIdsAndVersionFetchPartyRecord(partyRefDataIds, descItemTypes, changeId);
        for (ArrDataPartyRef data : partyData) {
            TitleValue value = new TitleValue(data.getParty().getRecord().getRecord());
            String iconValue = getIconValue(data);
            String code = data.getItem().getItemType().getCode();
            String specCode = data.getItem().getItemSpec() == null ? null : data.getItem().getItemSpec()
                    .getCode();
            Integer nodeId = data.getItem().getNodeId();
            Integer position = data.getItem().getPosition();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue, position);
        }

        List<ArrDataRecordRef> recordData = dataRecordRefRepository.findByDataIdsAndVersionFetchRecord(recordRefDataIds, descItemTypes, changeId);
        for (ArrDataRecordRef data : recordData) {
            TitleValue value = new TitleValue(data.getRecord().getRecord());
            String iconValue = getIconValue(data);
            String code = data.getItem().getItemType().getCode();
            String specCode = data.getItem().getItemSpec() == null ? null : data.getItem().getItemSpec()
                    .getCode();
            Integer nodeId = data.getItem().getNodeId();
            Integer position = data.getItem().getPosition();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue, position);
        }

        List<ArrDataPacketRef> packetData = dataPacketRefRepository.findByDataIdsAndVersionFetchPacket(packetRefDataIds, descItemTypes, changeId);
        for (ArrDataPacketRef data : packetData) {
            ArrPacket packet = data.getPacket();
            RulPacketType packetType = packet.getPacketType();
            TitleValue value;
            if (packetType == null) {
                value = new TitleValue(packet.getStorageNumber());
            } else {
                value = new TitleValue(packetType.getName() + ": " + packet.getStorageNumber());
            }
            String iconValue = getIconValue(data);
            String code = data.getItem().getItemType().getCode();
            String specCode = data.getItem().getItemSpec() == null ? null : data.getItem().getItemSpec()
                    .getCode();
            Integer nodeId = data.getItem().getNodeId();
            Integer position = data.getItem().getPosition();

            addValuesToMap(valueMap, value, code, specCode, nodeId, iconValue, position);
        }*/

        return valueMap;
    }

    private void addValuesToMap(final Map<Integer, Map<String, TitleValues>> valueMap, final TitleValue titleValue, final String code,
                                final String specCode, final Integer nodeId, final String iconValue, final Integer position) {

        if (titleValue == null && iconValue == null) {
            return;
        }

        Map<String, TitleValues> descItemCodeToValueMap = valueMap.get(nodeId);
        if (descItemCodeToValueMap == null) {
            descItemCodeToValueMap = new HashMap<>();
            valueMap.put(nodeId, descItemCodeToValueMap);
        }

        TitleValues titleValues = descItemCodeToValueMap.get(code);
        if (titleValues == null) {
            titleValues = new TitleValues();
            descItemCodeToValueMap.put(code, titleValues);
        }


        titleValue.setIconValue(iconValue);
        titleValue.setSpecCode(specCode);
        titleValue.setPosition(position);

        titleValues.addValue(titleValue);
    }


    private String getIconValue(final ArrDescItem data) {
        if (data.getItemSpec() != null) {
            return data.getItemSpec().getCode();
        }
        return null;
    }


    /**
     * Nahrazení textu v hodnotách textových atributů.
     *  @param version        verze stromu
     * @param descItemType   typ atributu
     * @param nodes          seznam uzlů, ve kterých hledáme
     * @param specifications seznam specifikací (pokud se jedná o typ atributu se specifikací)
     * @param findText       hledaný text v atributu
     * @param replaceText    text, který nahradí hledaný text v celém textu
     * @param allNodes       najít u všech JP a nahradit
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void replaceDescItemValues(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                      final RulItemType descItemType,
                                      final Set<ArrNode> nodes,
                                      final Set<RulItemSpec> specifications, final String findText,
                                      final String replaceText,
                                      final boolean allNodes) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        Assert.hasText(findText, "Musí být vyplněn hledaný text");

        Map<Integer, ArrNode> nodesMap = ElzaTools.createEntityMap(nodes, ArrNode::getNodeId);

        List<ArrDescItem> descItemsToReplaceText;
        if (allNodes) {
            descItemsToReplaceText = new LinkedList<>();

            Integer rootNodeId = version.getRootNode().getNodeId();
            Set<Integer> nodeIds = levelTreeCacheService.getAllNodeIdsByVersionAndParent(version, rootNodeId, ArrangementController.Depth.SUBTREE);
            nodeIds.add(rootNodeId);
            for (List<ArrNode> partNodes : Lists.partition(nodeRepository.findAll(nodeIds), 1000)) {
                descItemsToReplaceText.addAll(descItemRepository.findByNodesContainingText(partNodes, descItemType, specifications, findText));
            }
        } else {
            descItemsToReplaceText = descItemRepository.findByNodesContainingText(nodes, descItemType, specifications, findText);
        }

        if (!descItemsToReplaceText.isEmpty()) {


            ArrChange change = arrangementService.createChange(ArrChange.Type.BATCH_CHANGE_DESC_ITEM);

            for (ArrDescItem descItem: descItemsToReplaceText) {
                ArrNode clientNode = nodesMap.get(descItem.getNodeId());
                arrangementService.lockNode(descItem.getNode(), clientNode == null ? descItem.getNode() : clientNode, change);

                replaceDescItemValue(descItem, findText, replaceText, change);

                publishChangeDescItem(version, descItem);
            }
        }
    }

    public Class<? extends ArrData> getDescItemDataTypeClass(final RulItemType descItemType) {
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
                return ArrDataUnitid.class;
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
     * Vytvoří novou konkrétní instanci pro {@link ArrData}.
     *
     * @param descItemType typ atributu
     * @return konkrétní instance
     */
    private ArrData createDataByType(final RulItemType descItemType) {
        Class<? extends ArrData> dataTypeClass = getDescItemDataTypeClass(descItemType);
        try {
            return dataTypeClass.newInstance();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Nastavení textu hodnotám atributu.
     *  @param version              verze stromu
     * @param descItemType         typ atributu
     * @param newItemSpecification pokud se jedná o atribut se specifikací ->  specifikace, která bude nastavena
     * @param specifications       seznam specifikací, ve kterých se má hledat hodnota
     * @param text                 text, který nahradí text v celém textu
     * @param allNodes             vložit u všech JP
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void placeDescItemValues(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                    final RulItemType descItemType,
                                    final Set<ArrNode> nodes,
                                    final RulItemSpec newItemSpecification,
                                    final Set<RulItemSpec> specifications, final String text,
                                    final boolean allNodes) {
        Assert.hasText(text, "Musí být vyplněn text");
        Assert.isTrue(!descItemType.getUseSpecification() || newItemSpecification != null, "Neplatný stav specifikace");
        if (descItemType.getUseSpecification() && CollectionUtils.isEmpty(specifications)) {
            throw new BusinessException("Musí být zadána alespoň jedna filtrovaná specifikace.", BaseCode.PROPERTY_NOT_EXIST).set("property", "specifications");
        }


        Map<Integer, ArrNode> nodesMap = ElzaTools.createEntityMap(nodes, ArrNode::getNodeId);

        List<ArrDescItem> descItems;
        if (allNodes) {
            descItems = descItemType.getUseSpecification() ?
                    descItemRepository
                            .findOpenByFundAndTypeAndSpec(version.getFund(), descItemType, specifications) :
                    descItemRepository.findOpenByFundAndType(version.getFund(), descItemType);
        } else {
            descItems = descItemType.getUseSpecification() ?
                    descItemRepository
                            .findOpenByNodesAndTypeAndSpec(nodes, descItemType, specifications) :
                    descItemRepository.findOpenByNodesAndType(nodes, descItemType);
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.BATCH_CHANGE_DESC_ITEM);

        Map<Integer, List<Integer>> nodeDescItems = new HashMap<>();
        for (ArrDescItem descItem : descItems) {
            List<Integer> descItemList = nodeDescItems.computeIfAbsent(descItem.getNodeId(), k -> new ArrayList<>());
            descItemList.add(descItem.getDescItemObjectId());
            deleteDescriptionItem(descItem, version, change, false);
        }

        for (Map.Entry<Integer, List<Integer>> entry : nodeDescItems.entrySet()) {
            arrangementCacheService.deleteDescItems(entry.getKey(), entry.getValue());
        }

        //pokud má specifikaci a není opakovatelný, musíme zkontrolovat,
        //jestli nemá již nějakou hodnotu specifikace nastavenou (jinou než přišla v parametru seznamu specifikací)
        //takovým nodům nenastavujeme novou hodnotu se specifikací
        Set<ArrNode> ignoreNodes = new HashSet<>();
        if (descItemType.getUseSpecification() && BooleanUtils.isNotTrue(descItemType.getRepeatable())) {
            List<ArrDescItem> remainSpecItems = descItemRepository.findOpenByNodesAndType(nodes, descItemType);
            ignoreNodes = remainSpecItems.stream().map(ArrDescItem::getNode).collect(Collectors.toSet());
        }

        List<ArrNode> dbNodes;
        if (allNodes) {
            Integer rootNodeId = version.getRootNode().getNodeId();
            Set<Integer> nodeIds = levelTreeCacheService.getAllNodeIdsByVersionAndParent(version, rootNodeId, ArrangementController.Depth.SUBTREE);
            nodeIds.add(rootNodeId);
            dbNodes = nodeRepository.findAll(nodeIds);
        } else {
            dbNodes = nodeRepository.findAll(nodesMap.keySet());
        }

        for (ArrNode dbNode : dbNodes) {
            if (ignoreNodes.contains(dbNode)) {
                continue;
            }

            ArrNode arrNode = nodesMap.get(dbNode.getNodeId());
            arrangementService.lockNode(dbNode, arrNode == null ? dbNode : arrNode, change);

            ArrData data;
            switch (descItemType.getDataType().getCode()) {
                case "FORMATTED_TEXT":
                case "TEXT":
                    ArrDataText dataText = new ArrDataText();
                    dataText.setValue(text);
                    data = dataText;
                    break;
                case "STRING":
                    ArrDataString itemString = new ArrDataString();
                    itemString.setValue(text);
                    data = itemString;
                    break;
                case "INT":
                    ArrDataInteger itemInteger = new ArrDataInteger();
                    itemInteger.setValue(Integer.valueOf(text));
                    data = itemInteger;
                    break;
                case "UNITDATE":
                    ArrDataUnitdate itemUnitdate = createArrDataUnitdate(text);
                    data = itemUnitdate;
                    break;
                default:
                    throw new SystemException("Neplatný typ atributu " + descItemType.getDataType().getCode(), BaseCode.INVALID_STATE);
            }

            ArrDescItem newDescItem = new ArrDescItem();
            newDescItem.setData(data);
            newDescItem.setNode(dbNode);
            newDescItem.setItemType(descItemType);
            newDescItem.setItemSpec(newItemSpecification);
            newDescItem.setCreateChange(change);
            newDescItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
            newDescItem.setPosition(1);

            descItemFactory.saveDescItemWithData(newDescItem, true);
            arrangementCacheService.createDescItem(newDescItem.getNodeId(), newDescItem);
            publishChangeDescItem(version, newDescItem);
        }
    }

    /**
     * Vytvoření unitdate struktury.
     *
     * @param text vstupní text ve formátu '[calendarTypeId]|[unitDateText]'
     * @return vytvořená struktura
     */
    private ArrDataUnitdate createArrDataUnitdate(final String text) {
        String[] splitText = text.split("\\|", 2);

        if (splitText.length != 2 || !StringUtils.isNumeric(splitText[0])) {
            throw new SystemException("Neplatný vstupní řetězec: " + text, BaseCode.PROPERTY_IS_INVALID);
        }

        ArrCalendarType calendarType = calendarTypeRepository.findOne(Integer.valueOf(splitText[0]));

        if (calendarType == null) {
            throw new ObjectNotFoundException("Neexistující kalendář: " + splitText[0], BaseCode.ID_NOT_EXIST).setId(splitText[0]);
        }

        ArrDataUnitdate itemUnitdate = new ArrDataUnitdate();
        itemUnitdate.setCalendarType(calendarType);
        UnitDateConvertor.convertToUnitDate(splitText[1], itemUnitdate);

        CalendarConverter.CalendarType calendar = CalendarConverter.CalendarType.valueOf(calendarType.getCode());
        String value;
        value = itemUnitdate.getValueFrom();
        if (value != null) {
            itemUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendar, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        } else {
            itemUnitdate.setNormalizedFrom(Long.MIN_VALUE);
        }

        value = itemUnitdate.getValueTo();
        if (value != null) {
            itemUnitdate.setNormalizedTo(CalendarConverter.toSeconds(calendar, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        } else {
            itemUnitdate.setNormalizedTo(Long.MAX_VALUE);
        }
        return itemUnitdate;
    }

    /**
     * Provede nahrazení textu v hodnotě atributu.
     *
     * @param descItem      hodnota atributu
     * @param searchString  text, který hledáme
     * @param replaceString text, který nahradíme
     * @param change        změna (odverzování)
     */
    private void replaceDescItemValue(final ArrDescItem descItem, final String searchString, final String replaceString, final ArrChange change) {


        ArrData data = descItem.getData();
        ArrDescItem newDescItem = copyDescItem(change, descItem, descItem.getPosition());

        ArrData newData = createCopyDescItemData(data, newDescItem);


        switch (descItem.getItemType().getDataType().getCode()) {
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
                        "Zatím není implementováno pro kod " + descItem.getItemType().getCode());
        }

        dataRepository.save(newData);
    }


    /**
     * Smazání hodnot atributů daného typu pro vybrané uzly.
     *  @param version        verze stromu
     * @param descItemType   typ atributu, jehož hodnoty budou smazány
     * @param nodes          seznam uzlů, jejichž hodnoty mažeme
     * @param specifications seznam specifikací pro typ se specifikací, kterým budou smazány hodnoty
     * @param allNodes       odstranit u všech JP
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void deleteDescItemValues(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion version,
                                     final RulItemType descItemType,
                                     final Set<ArrNode> nodes,
                                     final Set<RulItemSpec> specifications,
                                     final boolean allNodes) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        if (descItemType.getUseSpecification() && CollectionUtils.isEmpty(specifications)) {
            throw new BusinessException("Musí být zadána alespoň jedna filtrovaná specifikace.", BaseCode.PROPERTY_NOT_EXIST).set("property", specifications);
        }

        List<ArrDescItem> descItems;
        if (allNodes) {
            descItems = descItemType.getUseSpecification() ?
                    descItemRepository
                            .findOpenByFundAndTypeAndSpec(version.getFund(), descItemType, specifications) :
                    descItemRepository.findOpenByFundAndType(version.getFund(), descItemType);
        } else {
            descItems = descItemType.getUseSpecification() ?
                    descItemRepository.findOpenByNodesAndTypeAndSpec(nodes, descItemType,
                            specifications) :
                    descItemRepository.findOpenByNodesAndType(nodes, descItemType);
        }

        if (!descItems.isEmpty()) {
            ArrChange change = arrangementService.createChange(ArrChange.Type.BATCH_DELETE_DESC_ITEM);

            Map<Integer, List<Integer>> nodeDescItems = new HashMap<>();

            for (ArrDescItem descItem : descItems) {
                List<Integer> descItemList = nodeDescItems.computeIfAbsent(descItem.getNodeId(), k -> new ArrayList<>());
                descItemList.add(descItem.getDescItemObjectId());
                deleteDescriptionItem(descItem, version, change, false);
            }

            for (Map.Entry<Integer, List<Integer>> entry : nodeDescItems.entrySet()) {
                arrangementCacheService.deleteDescItems(entry.getKey(), entry.getValue());
            }
        }
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
        Assert.notNull(text, "Musí být vyplněn text, ve kterém hledáme");
        Assert.notNull(searchString, "Hledaný text musí být vyplněn");

        return StringUtils.replace(text, searchString, replaceString);
    }


    /**
     * Vyhledá maximální pozici v hodnotách atributu podle typu.
     *
     * @param descItem hodnota atributu
     * @return maximální pozice (počet položek)
     */
    private int getMaxPosition(final ArrDescItem descItem) {
        int maxPosition = 0;
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getItemType(),
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
     * @param descItem     hodnota atributu
     * @param positionFrom od pozice (včetně)
     * @param positionTo   do pozice (včetně)
     * @return seznam nalezených hodnot atributů
     */
    private List<ArrDescItem> findDescItemsBetweenPosition(final ArrDescItem descItem,
                                                           final Integer positionFrom,
                                                           final Integer positionTo) {

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsBetweenPositions(descItem.getItemType(),
                descItem.getNode(), positionFrom, positionTo);

        return descItems;
    }

    /**
     * Nastaví hodnotu atributu na "Nezjištěno".
     *
     * @param descItemTypeId    identifikátor typu hodnoty atributu
     * @param nodeId            identifikátor uzlu
     * @param nodeVersion       verze uzlu (optimistické zámky)
     * @param fundVersionId     identifikátor verze archivní pomůcky
     * @param descItemSpecId    identifikátor specifikace hodnoty atributu
     * @param descItemObjectId  identifikátor hodnoty atributu
     * @return vytvořený atribut s příznamek "Nezjištěno"
     */
    public ArrDescItem setNotIdentifiedDescItem(final Integer descItemTypeId,
                                                final Integer nodeId,
                                                final Integer nodeVersion,
                                                final Integer fundVersionId,
                                                final Integer descItemSpecId,
                                                final Integer descItemObjectId) {
        ArrNode node = nodeRepository.findOne(nodeId);
        if (node == null) {
            throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + nodeId, ArrangementCode.NODE_NOT_FOUND).set("id", nodeId);
        }

        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        if (fundVersion == null) {
            throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId, ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
        }

        List<RulItemTypeExt> descriptionItemTypes = ruleService.getDescriptionItemTypes(fundVersion, node);
        RulItemType descItemType = descriptionItemTypes.stream().filter(rulItemTypeExt -> rulItemTypeExt.getItemTypeId().equals(descItemTypeId)).findFirst().orElse(null);
        if (descItemType == null) {
            throw new ObjectNotFoundException("Nebyla nalezen typ atributu s ID=" + nodeId, ArrangementCode.ITEM_TYPE_NOT_FOUND).set("id", descItemTypeId);
        }

        if (!descItemType.getIndefinable()) {
            throw new BusinessException("Položku není možné nastavit jako '" + ArrangementService.UNDEFINED + "'", ArrangementCode.CANT_SET_INDEFINABLE);
        }

        RulItemSpec descItemSpec = null;
        if (descItemSpecId != null) {
            descItemSpec = itemSpecRepository.findOne(descItemSpecId);
            if (descItemSpec == null) {
                throw new ObjectNotFoundException("Nebyla nalezena specifikace atributu s ID=" + descItemSpecId, ArrangementCode.ITEM_SPEC_NOT_FOUND).set("id", descItemSpecId);
            }
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.ADD_DESC_ITEM, node);

        if (descItemObjectId != null) {
            ArrDescItem openDescItem = descItemRepository.findOpenDescItem(descItemObjectId);
            if (openDescItem == null) {
                throw new ObjectNotFoundException("Nebyla nalezena hodnota atributu s OBJID=" + descItemObjectId, ArrangementCode.DATA_NOT_FOUND).set("descItemObjectId", descItemObjectId);
            } else if (openDescItem.isUndefined()) {
                throw new BusinessException("Položka již je nastavená jako '" + ArrangementService.UNDEFINED + "'", ArrangementCode.ALREADY_INDEFINABLE);
            }

            openDescItem.setDeleteChange(change);
            descItemRepository.save(openDescItem);
        }

        // uložení uzlu (kontrola optimistických zámků)
        node.setVersion(nodeVersion);
        saveNode(node, change);

        ArrDescItem descItem = new ArrDescItem();

        descItem.setNode(node);
        descItem.setItemType(descItemType);
        descItem.setItemSpec(descItemSpec);
        descItem.setCreateChange(change);
        descItem.setDeleteChange(null);
        descItem.setDescItemObjectId(descItemObjectId == null ? arrangementService.getNextDescItemObjectId() : descItemObjectId);

        ArrDescItem descItemCreated = createDescriptionItemWithData(descItem, fundVersion, change);

        // nastavujeme prázdné hodnoty
        //descItem.setItem(descItemFactory.createItemByType(descItemType.getDataType()));

        // validace uzlu
        ruleService.conformityInfo(fundVersion.getFundVersionId(), Collections.singletonList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, Collections.singletonList(descItem), null, null);

        // sockety
        publishChangeDescItem(fundVersion, descItemCreated);

        return descItemCreated;
    }
}
