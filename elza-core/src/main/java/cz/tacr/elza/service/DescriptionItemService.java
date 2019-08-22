package cz.tacr.elza.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.drools.DirectionLevel;
import cz.tacr.elza.drools.RulesExecutor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.search.IndexWorkProcessor;
import cz.tacr.elza.search.SearchIndexSupport;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventChangeDescItem;
import cz.tacr.elza.service.eventnotification.events.EventIdsInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.vo.TitleItemsByType;


/**
 * Description Item management
 *
 */
@Service
public class DescriptionItemService implements SearchIndexSupport<ArrDescItem> {

    private static final Logger logger = LoggerFactory.getLogger(DescriptionItemService.class);

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private RulesExecutor rulesExecutor;

    @Autowired
    private EventNotificationService notificationService;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private DescriptionItemServiceInternal serviceInternal;

    @Autowired
    private IndexWorkService indexWorkService;

    @Autowired
    private IndexWorkProcessor indexWorkProcessor;

    private TransactionSynchronizationAdapter indexWorkNotify;

    @PostConstruct
    public void init() {
        this.indexWorkNotify = new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                indexWorkProcessor.notifyIndexing();
            }
        };
    }

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
    public ArrNode saveNode(final ArrNode node, final ArrChange change) {
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
        Validate.notNull(fundVersion, "Verze archivní pomůcky neexistuje");

        StaticDataProvider sdp = staticDataService.getData();
        ItemType descItemType = sdp.getItemTypeById(descItemTypeId);
        Validate.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        ArrNode node = nodeRepository.findOne(nodeId);
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_DESC_ITEM, node);
        node.setVersion(nodeVersion);

        // uložení uzlu (kontrola optimistických zámků)
        saveNode(node, change);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemType.getEntity(), node);

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
        Validate.notNull(fundVersion, "Verze archivní pomůcky neexistuje");

        StaticDataProvider sdp = staticDataService.getData();
        ItemType descItemType = sdp.getItemTypeById(descItemTypeId);
        Validate.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        ArrNode node = nodeRepository.findOne(nodeId);
        ArrChange change = arrangementService.createChange(ArrChange.Type.DELETE_DESC_ITEM, node);

        List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemType.getEntity(), node);

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

        return createDescriptionItemsWithValidate(descItems, node, version, change);
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
	        final ArrChange createChange) {

		Validate.notNull(createChange);

        descItem.setNode(node);
		descItem.setCreateChange(createChange);
        descItem.setDeleteChange(null);
        descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

		ArrDescItem descItemCreated = createDescriptionItemWithData(descItem, version, createChange);

        // validace uzlu
        ruleService.conformityInfo(version.getFundVersionId(), Collections.singletonList(descItem.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, Collections.singletonList(descItem), null, null);

        // sockety
        publishChangeDescItem(version, descItemCreated);

        return descItemCreated;
    }

    public ArrDescItem createDescriptionItem(final ArrDescItem descItem,
                                             final Integer nodeId,
                                             final ArrFundVersion version,
                                             final ArrChange createChange) {
        ArrNode node = nodeRepository.getOne(nodeId);
        return createDescriptionItem(descItem, node, version, createChange);
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
    public List<ArrDescItem> createDescriptionItemsWithValidate(final List<ArrDescItem> descItems,
                                                                final ArrNode node,
                                                                final ArrFundVersion version,
                                                                @Nullable final ArrChange createChange) {

        List<ArrDescItem> createdItems = createDescriptionItems(descItems, node, version, createChange);

        // validace uzlu
        ruleService.conformityInfo(version.getFundVersionId(), Collections.singletonList(node.getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, createdItems, null, null);

        return createdItems;
    }

    /**
     * Vytvoření hodnoty atributu. Při ukládání nedojde ke zvýšení verze uzlu.
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

        return createdItems;
    }

    /**
     * Vytvoření hodnoty atributu s daty.
     *
     * @param descItem hodnota atributu
     * @param fundVersion  verze archivní pomůcky
     * @param change   změna operace
     * @return vytvořená hodnota atributu
     */
    public ArrDescItem createDescriptionItemWithData(final ArrDescItem descItem,
                                                     final ArrFundVersion fundVersion,
                                                     final ArrChange change) {
        Assert.notNull(descItem, "Hodnota atributu musí být vyplněna");
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(change, "Změna musí být vyplněna");


        // pro vytváření musí být verze otevřená
        checkFundVersionLock(fundVersion);

        StaticDataProvider sdp = staticDataService.getData();
        // kontrola validity typu a specifikace
        itemService.checkValidTypeAndSpec(sdp, descItem);

        int maxPosition = getMaxPosition(descItem);

        if (descItem.getPosition() == null || (descItem.getPosition() > maxPosition)) {
            descItem.setPosition(maxPosition + 1);
        }

        // načtení hodnot, které je potřeba přesunout níž
        //descItemRepository.flush();
        List<ArrDescItem> descItems = descItemRepository.findOpenDescItemsAfterPosition(
                descItem.getItemType(),
                descItem.getNode(),
                descItem.getPosition() - 1);

        List<ArrDescItem> descItemNews = new ArrayList<>(descItems.size());
        for (ArrDescItem descItemMove : descItems) {

			// make data copy
			ArrData trgData = copyDescItemData(descItemMove);
			// prepare new item and close old one
			ArrDescItem descItemNew = prepareNewDescItem(descItemMove, trgData, change);
			descItemNew.setPosition(descItemMove.getPosition() + 1);

            descItemRepository.save(descItemNew);

            descItemNews.add(descItemNew);
        }

        if (CollectionUtils.isNotEmpty(descItemNews)) {
            arrangementCacheService.changeDescItems(descItemNews.get(0).getNodeId(), descItemNews, true);
        }

        descItem.setCreateChange(change);
        descItemFactory.saveItemVersionWithData(descItem, true);

        arrangementCacheService.createDescItem(descItem.getNodeId(), descItem);
        return descItem;
    }


    /**
     * Smaže hodnotu atributu.
     *
     * Funkce současně posílá notifikaci přes WS
     * 
     * Function do not synchronize nodeCache !!!! Have to be synchronized manually
     * !!!
     * 
     * @param descItem
     *            hodnota atributu
     * @param version
     *            verze archivní pomůcky
     * @param change
     *            změna operace
     * @param moveAfter
     *            posunout hodnoty po?
     * @return smazaná hodnota atributu
     */
    //
    // Consider/TODO: Function should not sent WS events if nodeCache is not also synchronized?
    private ArrDescItem deleteDescriptionItem(final ArrDescItem descItem,
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
            // TODO: This functionality should be after item is deleted?
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
     * Odstraní požadované hodnoty atributů.
     *
     * @param descItemsToDelete
     *            hodnoty atributů k ostranění
     * @param node
     * @param fundVersion
     *            verze AS
     * @param change
     *            změna
     * @param moveAfter
     *            Flag to recalculate position of subsequent items
     *            If all items of same type are deleted position does
     *            not have to be recalculated
     * @return smazané hodnoty atributů
     */
    public List<ArrDescItem> deleteDescriptionItems(final List<ArrDescItem> descItemsToDelete,
                                                    final ArrNode node,
                                                    final ArrFundVersion fundVersion,
                                                    final ArrChange change,
                                                    final boolean moveAfter) {
        Validate.notEmpty(descItemsToDelete);
        Validate.notNull(fundVersion);
        Validate.notNull(change);

        List<Integer> itemObjectIds = descItemsToDelete.stream().map(ArrDescItem::getDescItemObjectId).collect(Collectors.toList());
        List<ArrDescItem> deleteDescItems = descItemRepository.findOpenDescItems(itemObjectIds);

        List<ArrDescItem> results = new ArrayList<>();
        for (ArrDescItem deleteDescItem : deleteDescItems) {
            ArrDescItem deletedItem = deleteDescriptionItem(deleteDescItem, fundVersion, change, moveAfter);

            results.add(deletedItem);
        }
        descItemRepository.flush();

        arrangementCacheService.deleteDescItems(node.getNodeId(), itemObjectIds);

        return results;
    }

    /**
	 * Provede posun (a odverzování) hodnot atributů jednoho uzlu s daty o
	 * požadovaný počet.
	 *
	 * @param change
	 *            změna operace
	 * @param descItems
	 *            seznam posunovaných hodnot atributu
	 * @param diff
	 *            počet a směr posunu
	 */
    private void copyDescItemsWithData(final ArrChange change, final List<ArrDescItem> descItems, final Integer diff,
                                       final ArrFundVersion version) {
        List<ArrDescItem> descItemNews = new ArrayList<>(descItems.size());
        for (ArrDescItem descItemMove : descItems) {

			// copy data
			ArrData trgData = copyDescItemData(descItemMove);

			// copy description item
			ArrDescItem descItemNew = prepareNewDescItem(descItemMove, trgData, change);
			descItemNew.setPosition(descItemMove.getPosition() + diff);
			descItemNew = descItemRepository.save(descItemNew);

            // sockety
            publishChangeDescItem(version, descItemNew);
        }

        if (CollectionUtils.isNotEmpty(descItemNews)) {
            arrangementCacheService.changeDescItems(descItemNews.get(0).getNodeId(), descItemNews, true);
        }
    }

    /**
	 * Prepare new version of description item. Current description item is
	 * marked as deleted and saved.
	 *
	 * Returned item is not saved. Caller can modify returned item before saving
	 * and then have to save it.
	 *
	 * @param descItem
	 *            source item
	 * @param trgData
	 *            new data for returned copy
	 * @param change
	 *            změna, se kterou dojde k uzamčení a vytvoření kopie
	 * @return copy of the source item with new data. Returned item is not
	 *         saved!!!
	 */
	private ArrDescItem prepareNewDescItem(final ArrDescItem descItem, final ArrData trgData, final ArrChange change) {
		// Mark orig descItem as deleted
		descItem.setDeleteChange(change);
        descItemRepository.save(descItem);

		// Create new one
		ArrDescItem descItemNew = new ArrDescItem(descItem);

        descItemNew.setItemId(null);
		descItemNew.setData(trgData);
        descItemNew.setDeleteChange(null);
        descItemNew.setCreateChange(change);

		return descItemNew;
    }

    /**
     * Vytvoří kopii prvků popisu. Kopírovaný atribut patří zvolenému uzlu.
     *
     * Method will also update nodeCache.
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

			// copy and save data
			ArrData trgData = copyDescItemData(sourceDescItem);
			// prepare new description item
			// first make copy from original
			ArrDescItem descItemNew = new ArrDescItem(sourceDescItem);
			// set data
			descItemNew.setData(trgData);
			// set parent node
            descItemNew.setNode(node);
			// reset id
            descItemNew.setItemId(null);
			// prepare change info
            descItemNew.setDeleteChange(null);
            descItemNew.setCreateChange(createChange);
			// prepare new object id
            descItemNew.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

            descItemRepository.save(descItemNew);

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
	 * Provede kopii dat ze zdrojové item
	 *
	 * Function will save created data in repository
	 *
	 * @param itemFrom
	 *            z hodnoty atributu
	 * @return Return object with data copy. If data are not defined in source
	 *         item method will return null.
	 */
	private ArrData copyDescItemData(final ArrItem itemFrom) {
		ArrData srcData = itemFrom.getData();
		if (srcData == null) {
			return null;
		}
		ArrData dataNew = ArrData.makeCopyWithoutId(srcData);
		return dataRepository.save(dataNew);
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

		ArrChange change = null;
		ArrDescItem descItemUpdated;
        if (createNewVersion) {
            node.setVersion(nodeVersion);

            // vytvoření změny
            change = arrangementService.createChange(ArrChange.Type.UPDATE_DESC_ITEM, node);

            // uložení uzlu (kontrola optimistických zámků)
            saveNode(node, change);

			descItemUpdated = updateItemValueAsNewVersion(fundVersion, change, descItemDB, descItem.getItemSpec(),
			        descItem.getData());
		} else {
			descItemUpdated = updateValue(fundVersion, descItem);
        }

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
        ArrLevel level = levelRepository.findByNode(node, version.getLockChange());

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

		ArrDescItem descItemUpdated = updateValueAsNewVersion(fundVersion, change, descItem);

        // validace uzlu
        ruleService.conformityInfo(fundVersion.getFundVersionId(), Arrays.asList(descItemUpdated.getNode().getNodeId()),
                NodeTypeOperation.SAVE_DESC_ITEM, null, Arrays.asList(descItemUpdated), null);

        return descItemUpdated;
    }

    /**
     * Hromadná úprava hodnot atributů.
     *
     * @param updateDescItems hodnoty atributů k úpravě
     * @param fundVersion     verze AS
     * @param change          změna
     * @return upravěné hodnoty
     */
    public List<ArrDescItem> updateDescriptionItems(final List<ArrDescItem> updateDescItems,
                                                    final ArrFundVersion fundVersion,
                                                    final ArrChange change) {
        List<ArrDescItem> results = new ArrayList<>();
        for (ArrDescItem updateDescItem : updateDescItems) {
            results.add(updateValueAsNewVersion(fundVersion, change, updateDescItem));
        }
        descItemRepository.flush();
        return results;
    }

    /**
	 * Fetch open item with given ID from DB
	 *
	 * @param descItemObjectId
	 * @return
	 */
	protected ArrDescItem fetchOpenItemFromDB(int descItemObjectId) {
		// fetch item from DB
		List<ArrDescItem> descItems = descItemRepository.findOpenDescItems(descItemObjectId);

		if (descItems.size() > 1) {
			throw new SystemException("Hodnota musí být právě jedna", BaseCode.DB_INTEGRITY_PROBLEM);
		} else if (descItems.size() == 0) {
			throw new SystemException("Hodnota neexistuje, pravděpodobně byla již smazána");
		}
		ArrDescItem descItem = descItems.get(0);
		return descItem;
	}

	/**
	 * Update value without creating new version
	 *
	 * Method will update specification and value
	 *
	 * @param descItem
	 *            detached item with new value
	 */
	public ArrDescItem updateValue(final ArrFundVersion version, final ArrDescItem descItem)
    {
		// fetch item from DB
		ArrDescItem descItemCurr = fetchOpenItemFromDB(descItem.getDescItemObjectId());

		// item type have to be same
		if (!Objects.equal(descItemCurr.getItemTypeId(), descItem.getItemTypeId())) {
			throw new SystemException("Different item types, cannot update value");
		}
		// position have to be same
		if (!Objects.equal(descItemCurr.getPosition(), descItem.getPosition())) {
			throw new SystemException("Different item positions, cannot update value");
		}

		ArrData data = descItem.getData();
		// save new data
		data = descItemFactory.saveData(descItem.getItemType(), data);
		// update item
		return updateValue(version, descItemCurr, descItem.getItemSpec(), data);
	}

	/**
	 * Internal method to update item and data
	 *
	 * Input is attached entity.
	 *
	 * @param descItemDB
	 * @param data
	 * @return
	 */
    private ArrDescItem updateValue(final ArrFundVersion fundVersion, ArrDescItem descItemDB, RulItemSpec itemSpec,
	        ArrData data) {


        StaticDataProvider sdp = staticDataService.getData();

        // set data and specification
		descItemDB.setData(data);
		descItemDB.setItemSpec(itemSpec);
		ArrDescItem result = descItemRepository.save(descItemDB);

        itemService.checkValidTypeAndSpec(sdp, result);

		// update value in node cache
		arrangementCacheService.changeDescItem(result.getNodeId(), result, false);

		// sockety
        publishChangeDescItem(fundVersion, result);

		return result;
	}

	/**
	 * Versionable update
	 *
	 * This method will create new item version. Item type and position of the
	 * item cannot be changed.
	 *
	 * @param descItem
	 *            detached item with new value
	 *
	 */
	public ArrDescItem updateValueAsNewVersion(final ArrFundVersion version, final ArrChange change,
	        final ArrDescItem descItem) {
		// fetch item from DB
		ArrDescItem descItemCurr = fetchOpenItemFromDB(descItem.getDescItemObjectId());

		// item type have to be same
		if (!Objects.equal(descItemCurr.getItemTypeId(), descItem.getItemTypeId())) {
			throw new SystemException("Different item types, cannot update value");
		}
		// position have to be same
		if (!Objects.equal(descItemCurr.getPosition(), descItem.getPosition())) {
			throw new SystemException("Different item positions, cannot update value");
		}

		// save new data
		ArrData dataCurr = descItem.getData();

		return updateItemValueAsNewVersion(version, change, descItemCurr, descItem.getItemSpec(), dataCurr);
	}

	private ArrDescItem updateItemValueAsNewVersion(final ArrFundVersion version, final ArrChange change,
	        final ArrDescItem descItemDB, RulItemSpec itemSpec,
	        ArrData srcData) {

		ArrData dataNew = descItemFactory.saveData(descItemDB.getItemType(), srcData);

		// create new item based on source
		ArrDescItem descItemNew = new ArrDescItem(descItemDB);
		descItemNew.setItemId(null);
		descItemNew.setCreateChange(change);

		// mark current item as deleted and save
		descItemDB.setDeleteChange(change);
		descItemRepository.save(descItemDB);

		// save new item
		ArrDescItem descItemUpdated = updateValue(version, descItemNew, itemSpec, dataNew);

		return descItemUpdated;

	}

    public Map<Integer, TitleItemsByType> createNodeValuesByItemTypeIdMap(final Collection<Integer> nodeIds,
                                                                                   final Collection<RulItemType> descItemTypes,
                                                                                   final Integer changeId,
                                                                                   @Nullable final TreeNode subtreeRoot,
                                                                                   final boolean dataExport) {
        if (nodeIds.isEmpty() || descItemTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Integer> nodeIdSet = new HashSet<>(nodeIds);

        // chceme nalézt atributy i pro rodiče podstromu
        TreeNode rootParent = subtreeRoot;
        while (rootParent != null) {
            nodeIdSet.add(rootParent.getId());
            rootParent = rootParent.getParent();
        }

        List<ArrDescItem> descItems = descItemRepository.findDescItemsByNodeIds(nodeIdSet, descItemTypes, changeId);

        Map<Integer, TitleItemsByType> nodeIdMap = new HashMap<>();
        for (ArrDescItem descItem : descItems) {
            TitleValue titleValue = serviceInternal.createTitleValue(descItem, dataExport);
            Integer nodeId = descItem.getNodeId();

            TitleItemsByType itemsByType = nodeIdMap.computeIfAbsent(nodeId, id -> new TitleItemsByType());

            itemsByType.addItem(descItem.getItemTypeId(), titleValue);
        }
        return nodeIdMap;
    }

    /**
     * 
     * @param nodeIds
     * @param descItemTypes
     * @param changeId
     * @param subtreeRoot
     * @return Map of
     *         nodeId, itemTypeCode, values
     */
    public Map<Integer, TitleItemsByType> createNodeValuesByItemTypeCodeMap(final Collection<Integer> nodeIds,
                                                                                    final Collection<RulItemType> descItemTypes,
                                                                                    final Integer changeId,
                                                                                    @Nullable final TreeNode subtreeRoot) {

        return createNodeValuesByItemTypeIdMap(nodeIds, descItemTypes, changeId, subtreeRoot, false);
    }

    /**
     * Nahrazení textu v hodnotách textových atributů.
     * @param version        verze stromu
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
            case "DATE":
                return ArrDataDate.class;
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
            case "STRUCTURED":
                return ArrDataStructureRef.class;
            case "ENUM":
                return ArrDataNull.class;
            default:
                throw new NotImplementedException("Nebyl namapován datový typ");
        }
    }

    /**
     * Nastavení textu hodnotám atributu.
     * @param version              verze stromu
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
        if (descItemType.getUseSpecification() && BooleanUtils.isNotTrue(descItemType.getRepeatable()) && nodes.size() > 0) {
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
                case "UNITID":
                    ArrDataUnitid itemUnitid = new ArrDataUnitid();
                itemUnitid.setUnitId(text);
                    data = itemUnitid;
                    break;
                case "UNITDATE":
                    ArrDataUnitdate itemUnitdate = createArrDataUnitdate(text);
                    data = itemUnitdate;
                    break;
                case "RECORD_REF":
                    ArrDataRecordRef itemRecordRef = new ArrDataRecordRef();
                    ApAccessPoint record = apAccessPointRepository.getOneCheckExist(Integer.valueOf(text));
                    itemRecordRef.setRecord(record);
                    data = itemRecordRef;
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

            descItemFactory.saveItemVersionWithData(newDescItem, true);
            arrangementCacheService.createDescItem(newDescItem.getNodeId(), newDescItem);
            publishChangeDescItem(version, newDescItem);
        }
    }

    /**
     * Nastavit specifikaci hodnotám atributu.
     *
     * @param fundVersion      verze stromu
     * @param itemType         typ atributu
     * @param setSpecification specifikace, která bude nastavena
     * @param specifications   seznam specifikací, ve kterých se má hledat hodnota
     * @param allNodes         vložit u všech JP
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void setSpecification(@AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                 final RulItemType itemType,
                                 final Set<ArrNode> nodes,
                                 final RulItemSpec setSpecification,
                                 final Set<RulItemSpec> specifications,
                                 final boolean setNull,
                                 final boolean allNodes) {
        Assert.notNull(itemType, "Typ atributu musí být vyplněn");
        Assert.notNull(setSpecification, "Nastavovaná specifikace musí být vyplněna");
        if (!itemType.getUseSpecification()) {
            throw new BusinessException("Typ atributu nemá povolené specifikace.", BaseCode.PROPERTY_NOT_EXIST).set("property", "itemTypeId");
        }
        if (setNull) {
            if (!itemType.getDataType().getCode().equals("ENUM")) {
                throw new BusinessException("Typ atributu musí být typu enum.", BaseCode.PROPERTY_IS_INVALID).set("property", "itemTypeId");
            }
        }

        List<ArrDescItem> descItems;
        Set<Integer> nodeIdsToAdd;
        if (allNodes) {
            Integer rootNodeId = fundVersion.getRootNode().getNodeId();
            nodeIdsToAdd = levelTreeCacheService.getAllNodeIdsByVersionAndParent(fundVersion, rootNodeId, ArrangementController.Depth.SUBTREE);
            nodeIdsToAdd.add(rootNodeId);
            descItems = specifications.size() == 0 ? Collections.emptyList() : descItemRepository.findOpenByFundAndTypeAndSpec(fundVersion.getFund(), itemType, specifications);
            List<ArrDescItem> ignoreDescItems = descItemRepository.findOpenByFundAndType(fundVersion.getFund(), itemType);
            ignoreDescItems.removeAll(descItems);
            for (ArrDescItem ignoreDescItem : ignoreDescItems) {
                nodeIdsToAdd.remove(ignoreDescItem.getNodeId());
            }
        } else {
            descItems = specifications.size() == 0 ? Collections.emptyList() : descItemRepository.findOpenByNodesAndTypeAndSpec(nodes, itemType, specifications);
            nodeIdsToAdd = nodes.stream().map(ArrNode::getNodeId).collect(Collectors.toSet());
            List<ArrDescItem> ignoreDescItems = descItemRepository.findOpenByNodesAndType(nodes, itemType);
            ignoreDescItems.removeAll(descItems);
            for (ArrDescItem ignoreDescItem : ignoreDescItems) {
                nodeIdsToAdd.remove(ignoreDescItem.getNodeId());
            }
        }

        ArrChange change = arrangementService.createChange(ArrChange.Type.BATCH_CHANGE_DESC_ITEM);

        List<Integer> nodeIds = new ArrayList<>();
        List<ArrDescItem> updatedDescItems = new ArrayList<>(descItems.size());
        for (ArrDescItem descItem : descItems) {
			ArrDescItem updatedDescItem = updateItemValueAsNewVersion(fundVersion, change, descItem, setSpecification,
			        descItem.getData());
            nodeIds.add(updatedDescItem.getNodeId());
            nodeIdsToAdd.remove(updatedDescItem.getNodeId());
            updatedDescItems.add(updatedDescItem);
        }

        List<ArrDescItem> createdDescItems = new ArrayList<>(nodeIdsToAdd.size());
        if (setNull) {
            for (Integer nodeId : nodeIdsToAdd) {
                ArrDescItem descItem = new ArrDescItem();
                descItem.setData(new ArrDataNull());
                descItem.setNode(nodeRepository.getOne(nodeId));
                descItem.setItemType(itemType);
                descItem.setItemSpec(setSpecification);
                descItem.setCreateChange(change);
                descItem.setDeleteChange(null);
                descItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
                nodeIds.add(nodeId);
                ArrDescItem created = createDescriptionItemWithData(descItem, fundVersion, change);
                createdDescItems.add(created);
            }
        }

        if (nodeIds.size() > 0) {
            eventNotificationService.publishEvent(new EventIdsInVersion(EventType.NODES_CHANGE, fundVersion.getFundVersionId(), nodeIds.toArray(new Integer[nodeIds.size()])));
            // validace uzlu
            ruleService.conformityInfo(fundVersion.getFundVersionId(), nodeIds,
                    NodeTypeOperation.SAVE_DESC_ITEM, createdDescItems, updatedDescItems, null);
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

        CalendarType calendar = CalendarType.valueOf(calendarType.getCode());
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
		Validate.notNull(data, "item without data");

		ArrData dataNew = ArrData.makeCopyWithoutId(data);

		// modify data
		if (dataNew instanceof ArrDataString) {
			ArrDataString ds = (ArrDataString) dataNew;
			ds.setValue(getReplacedDataValue(ds.getValue(), searchString, replaceString));
		} else if (dataNew instanceof ArrDataText) {
			ArrDataText dt = (ArrDataText) dataNew;
			dt.setValue(getReplacedDataValue(dt.getValue(), searchString, replaceString));
		} else if (dataNew instanceof ArrDataUnitid) {
            ArrDataUnitid dt = (ArrDataUnitid) dataNew;
            dt.setUnitId(getReplacedDataValue(dt.getUnitId(), searchString, replaceString));
        } else {
			throw new IllegalStateException(
			        "Zatím není implementováno pro kod " + descItem.getItemType().getCode());
		}
		dataNew = dataRepository.save(dataNew);

		ArrDescItem descItemNew = prepareNewDescItem(descItem, dataNew, change);
		descItemNew = descItemRepository.save(descItemNew);
        arrangementCacheService.changeDescItem(descItem.getNodeId(), descItem, false);
    }


    /**
     * Smazání hodnot atributů daného typu pro vybrané uzly.
     * @param version        verze stromu
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
        ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
        if (fundVersion == null) {
            throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId, ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
        }

        ArrNode node = nodeRepository.findOne(nodeId);
        if (node == null) {
            throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + nodeId, ArrangementCode.NODE_NOT_FOUND)
                    .set("id", nodeId);
        }

        StaticDataProvider sdp = staticDataService.getData();
        ItemType descItemType = sdp.getItemTypeById(descItemTypeId);
        Validate.notNull(descItemType, "Typ hodnoty atributu neexistuje");

        RulItemSpec descItemSpec = null;
        if (descItemSpecId != null) {
            descItemSpec = descItemType.getItemSpecById(descItemSpecId);
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
        descItem.setItemType(descItemType.getEntity());
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

    @Override
    public Map<Integer, ArrDescItem> findToIndex(Collection<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        // moznost optimalizovat nacteni vcene zavislosti
        return descItemRepository.findAll(ids).stream().collect(Collectors.toMap(o -> o.getItemId(), o -> o));
    }

    @Transactional
    public void reindexDescItem(Integer itemId) {
        try {
            indexWorkService.createIndexWork(ArrDescItem.class, itemId);
        } finally {
            TransactionSynchronizationManager.registerSynchronization(indexWorkNotify);
        }
    }

    @Transactional
    public void reindexDescItem(Collection<Integer> itemIds) {
        try {
            indexWorkService.createIndexWork(ArrDescItem.class, itemIds);
        } finally {
            TransactionSynchronizationManager.registerSynchronization(indexWorkNotify);
        }
    }

}
