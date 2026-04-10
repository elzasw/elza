package cz.tacr.elza.service;

import static cz.tacr.elza.repository.ExceptionThrow.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.ArrangementController.DescItemResult;
import cz.tacr.elza.controller.arrangement.UpdateItemResult;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.NodeFormData;
import cz.tacr.elza.controller.vo.ItemDataResult;
import cz.tacr.elza.controller.vo.FormItemType;
import cz.tacr.elza.controller.vo.NodeBase;
import cz.tacr.elza.controller.vo.NodeItem;
import cz.tacr.elza.controller.vo.NodeUpdateItem;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.vo.UpdateDescItemsParam;
import cz.tacr.elza.websocket.service.WebSoсketStompService;

/**
 * Service to handle form related requests
 *
 * Service is checking user rights.
 */
@Service
public class ArrangementFormService {

	private static final Logger logger = LoggerFactory.getLogger(ArrangementFormService.class);

	private final StaticDataService staticData;

	private final DescriptionItemServiceInternal arrangementInternal;

	private final DescriptionItemService descriptionItemService;

	private final RuleService ruleService;

	private final LevelTreeCacheService levelTreeCache;

	private final NodeRepository nodeRepository;

	private final ClientFactoryDO factoryDo;

	private final ClientFactoryVO factoryVo;

	private final WebSoсketStompService wsStompService;

	private final NodeCacheService nodeCacheService;

	private final ArrangementService arrangementService;

	private final ArrangementInternalService arrangementInternalService;

    private final UserService userService;

	public ArrangementFormService(StaticDataService staticData,
								  DescriptionItemServiceInternal arrangementInternal,
								  DescriptionItemService descriptionItemService,
								  LevelTreeCacheService levelTreeCache,
								  UserService userService,
								  RuleService ruleService,
								  WebSoсketStompService wsStompService,
								  ClientFactoryVO factoryVo,
								  ClientFactoryDO factoryDo,
								  NodeCacheService nodeCache,
								  FundVersionRepository fundVersionRepository,
								  NodeRepository nodeRepository,
								  final ArrangementService arrangementService,
								  final ArrangementInternalService arrangementInternalService) {
		this.staticData = staticData;
		this.arrangementInternal = arrangementInternal;
		this.descriptionItemService = descriptionItemService;
		this.levelTreeCache = levelTreeCache;
		this.ruleService = ruleService;
		this.nodeRepository = nodeRepository;
		this.factoryDo = factoryDo;
		this.factoryVo = factoryVo;
		this.nodeCacheService = nodeCache;
		this.wsStompService = wsStompService;
		this.arrangementService = arrangementService;
		this.arrangementInternalService = arrangementInternalService;
        this.userService = userService;
	}

	@Deprecated
	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public DescFormDataNewVO getNodeFormDataOld(@AuthParam(type = AuthParam.Type.FUND_VERSION) Integer versionId, Integer nodeId) {
		ArrFundVersion version = arrangementService.getFundVersion(versionId);
		return getNodeFormDataOld(version, nodeId);
	}

	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public NodeFormData getNodeFormData(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion version, Integer nodeId) {
		// získat seznam rodičovských nodů
		List<Integer> parentNodeIds = this.levelTreeCache.getParentNodes(version, nodeId);
		
		ArrChange lockChange = version.getLockChange();
		ArrNode node;
		List<ArrDescItem> descItems = new ArrayList<>(); 
		Set<Integer> inhibitedDescItemIds;
		Set<Integer> inhibitedDescItemObjectIds;
		List<ArrDescItem> parentsDescItems;
		RestoredNode restoredNode = null;		
		if (lockChange == null) {
			// read node from cache
			restoredNode = nodeCacheService.getNode(nodeId);
			node = restoredNode.getNode();			
		} else {
			// read node from db
			node = nodeRepository.findById(nodeId).orElseThrow(node(nodeId));
		}

		List<RulItemTypeExt> itemTypes;
		Set<Integer> itemTypeIdsWithInheritance = new HashSet<>();
		try {
			itemTypes = ruleService.getDescriptionItemTypes(version, node);
			// Add itemTypeId with inheritance to set (from rules)
			for(RulItemTypeExt itemType : itemTypes) {
				if(itemType.isInheritance()) {
					itemTypeIdsWithInheritance.add(itemType.getItemTypeId());
				}
			}
		} catch (Exception e) {
			logger.error("Chyba v pravidlech", e);
			throw new BusinessException("Chyba v pravidlech", e, BaseCode.SYSTEM_ERROR);
		}

		if (lockChange == null) {
			// get descItems from cache
			var restoredDescItems = restoredNode.getDescItems();
			if(restoredDescItems!=null) {
				descItems.addAll(restoredDescItems);
			}
			// read parent nodes
			Collection<RestoredNode> parentRestoredNodes = nodeCacheService.getNodes(parentNodeIds).values();
			// map descItemObjectId -> ArrDescItem pro rychlé hledání záznamů s potlačenou dědičností
		    Map<Integer, ArrDescItem> descItemObjectIdMap = parentRestoredNodes.stream().flatMap(i -> i.getDescItems()!=null?i.getDescItems().stream():Stream.empty())
		    		.collect(Collectors.toMap(i -> i.getDescItemObjectId(), Function.identity()));
		    		
			// Add any inhibited item from node to set
			inhibitedDescItemObjectIds = new HashSet<>();
			for(var inhItem: restoredNode.getInhibitedItems()) {
				var srcItem = descItemObjectIdMap.get(inhItem.getDescItemObjectId());
				if(srcItem == null) {
					throw new SystemException("Inhibited item not found in parent nodes", BaseCode.DB_INTEGRITY_PROBLEM)
						.set("descItemObjectId", inhItem.getDescItemObjectId())
						.set("nodeId", restoredNode.getNode().getNodeId());
				}
				itemTypeIdsWithInheritance.add(srcItem.getItemTypeId());
				
				// seznam descItemObjectId s potlačenou dědičností pro aktuální uzel
				inhibitedDescItemObjectIds.add(inhItem.getDescItemObjectId());
			}			
			// v uzlu, kde je dědičnost potlačena, stále zobrazujeme zděděné záznamy
			// sbíráme id záznamy (descItemId) s potlačenou dědičností od nadřazených uzlů
			inhibitedDescItemIds = getInhibitedDescItemIds(parentRestoredNodes);
			// sbíráme všechny descItems s povolenou dědičností z nadřazených uzlů
			parentsDescItems = parentRestoredNodes.stream()
					.flatMap(i -> i.getDescItems()!=null?i.getDescItems().stream():Stream.empty())
					.filter(i -> itemTypeIdsWithInheritance.contains(i.getDescItemTypeId()))
					.toList();
		} else {
			var restoredDescItems = arrangementInternal.getDescItems(lockChange, node);
			if(restoredDescItems!=null) {
				descItems.addAll(restoredDescItems);
			}
			
			inhibitedDescItemIds = arrangementInternal.getInhibitedDescItemIds(lockChange, parentNodeIds);
			parentsDescItems = descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(parentNodeIds, itemTypeIdsWithInheritance);
			inhibitedDescItemObjectIds = arrangementInternal.getInhibitedDescItemObjectIds(lockChange, List.of(nodeId));
		}

		// získat seznam descItems, které lze zdědit
		for (ArrDescItem descItem : parentsDescItems) {
			// pokud typ prvku umožňuje dědění (již filtrováno) & dědičnost tohoto prvku není potlačena
			if (!inhibitedDescItemIds.contains(descItem.getItemId())) {
				// přidat tento prvek do seznamu
				descItems.add(descItem);
			}
		}

		Integer fundId = version.getFund().getFundId();
		String ruleCode = version.getRuleSet().getCode();

		NodeBase nodeParent = new NodeBase(node.getNodeId(), node.getVersion(), node.getUuid());
		List<NodeItem> nodeItems = factoryVo.createNodeItems(nodeId, descItems, inhibitedDescItemObjectIds);
		List<FormItemType> formItemTypes = factoryVo.createFormItemTypes(ruleCode, fundId, itemTypes);

        boolean arrPerm = userService.hasFullArrPerm(version.getFundId());
		if (!arrPerm) {
			Map<Integer, Boolean> permNodeIdMap = levelTreeCache.calcPermNodeIdMap(version, Collections.singleton(nodeId));
			arrPerm = permNodeIdMap.get(nodeId);
		}
		return new NodeFormData(nodeParent, nodeItems, formItemTypes, arrPerm);
	}

	@Deprecated
	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public DescFormDataNewVO getNodeFormDataOld(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion version, Integer nodeId) {

		// získat seznam rodičovských nodů
		List<Integer> parentNodeIds = this.levelTreeCache.getParentNodes(version, nodeId);
		
		ArrChange lockChange = version.getLockChange();
		ArrNode node;
		List<ArrDescItem> descItems;
		Set<Integer> inhibitedDescItemIds;
		Set<Integer> inhibitedDescItemObjectIds;
		List<ArrDescItem> parentsDescItems;
		RestoredNode restoredNode = null;
		Collection<RestoredNode> parentRestoredNodes = new ArrayList<>();
		if (lockChange == null) {
			// read node from cache
			restoredNode = nodeCacheService.getNode(nodeId);
			node = restoredNode.getNode();
			parentRestoredNodes = nodeCacheService.getNodes(parentNodeIds).values();
		} else {
			// read node from db
			node = nodeRepository.findById(nodeId).orElseThrow(node(nodeId));
			// TODO: add support for inheritence for nodes from DB
		}

		List<RulItemTypeExt> itemTypes;
		try {
			itemTypes = ruleService.getDescriptionItemTypes(version, node);
		} catch (Exception e) {
			logger.error("Chyba v pravidlech", e);
			throw new BusinessException("Chyba v pravidlech", e, BaseCode.SYSTEM_ERROR);
		}

		Set<Integer> itemTypeIdsWithInheritance = itemTypes.stream()
				.filter(i -> i.isInheritance())
				.map(i -> i.getItemTypeId())
				.collect(Collectors.toSet());
		if (lockChange == null) {
			descItems = restoredNode.getDescItems();
			
			// v uzlu, kde je dědičnost potlačena, stále zobrazujeme zděděné záznamy
			// sbíráme id záznamy (descItemId) s potlačenou dědičností od nadřazených uzlů
			inhibitedDescItemIds = getInhibitedDescItemIds(parentRestoredNodes);
			// sbíráme všechny descItems s povolenou dědičností z nadřazených uzlů
			parentsDescItems = parentRestoredNodes.stream()
					.flatMap(i -> i.getDescItems()!=null?i.getDescItems().stream():Stream.empty())
					.filter(i -> itemTypeIdsWithInheritance.contains(i.getDescItemTypeId()))
					.toList();
			// seznam descItemId s potlačenou dědičností pro aktuální uzel
			inhibitedDescItemObjectIds = restoredNode.getInhibitedItems().stream().map(i -> i.getDescItemObjectId()).collect(Collectors.toSet());
		} else {
			descItems = arrangementInternal.getDescItems(lockChange, node);
			inhibitedDescItemIds = arrangementInternal.getInhibitedDescItemIds(lockChange, parentNodeIds);
			parentsDescItems = descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(parentNodeIds, itemTypeIdsWithInheritance);
			inhibitedDescItemObjectIds = arrangementInternal.getInhibitedDescItemObjectIds(lockChange, List.of(nodeId));
		}

		// získat seznam descItems, které lze zdědit
		for (ArrDescItem descItem : parentsDescItems) {
			// pokud typ prvku umožňuje dědění (již filtrováno) & dědičnost tohoto prvku není potlačena
			if (!inhibitedDescItemIds.contains(descItem.getItemId())) {
				// přidat tento prvek do seznamu
				descItems.add(descItem);
			}
		}

		Integer fundId = version.getFund().getFundId();
		String ruleCode = version.getRuleSet().getCode();

		ArrNodeVO nodeVO = ArrNodeVO.valueOf(node);
		List<ArrItemVO> descItemsVOs = factoryVo.createItems(nodeId, descItems, inhibitedDescItemObjectIds);
		List<ItemTypeLiteVO> itemTypeLites = factoryVo.createItemTypes(ruleCode, fundId, itemTypes);

        boolean arrPerm = userService.hasFullArrPerm(version.getFundId());
		if (!arrPerm) {
			Map<Integer, Boolean> permNodeIdMap = levelTreeCache.calcPermNodeIdMap(version, Collections.singleton(nodeId));
			arrPerm = permNodeIdMap.get(nodeId);
		}
		return new DescFormDataNewVO(nodeVO, descItemsVOs, itemTypeLites, arrPerm);
	}
	
	/**
	 * Získání seznamu ID (itemId) s potlačenou dědičností ze seznamu uzlů
	 *
	 * @param restoredNodes
	 * @param descItemObjectIdMap mapa descItemObjectId -> ArrDescItem pro rychlé hledání záznamů s potlačenou dědičností
	 * @return
	 */
	private Set<Integer> getInhibitedDescItemIds(Collection<RestoredNode> restoredNodes) {
		// list of descItemObjectId s potlačenou dědičností pro nadrazene uzly
		Set<Integer> descItemObjectIds = restoredNodes.stream()
				.flatMap(i -> i.getInhibitedItems()!=null? i.getInhibitedItems().stream() : Stream.empty())
				.map(i -> i.getDescItemObjectId())
				.collect(Collectors.toSet());
		
		Set<Integer> inhibitedDescItemIds = new HashSet<Integer>();
		for (RestoredNode node : restoredNodes) {
            if (node.getDescItems() == null) {
                continue;
            }
			for (ArrDescItem descItem : node.getDescItems()) {
				if (descItemObjectIds.contains(descItem.getDescItemObjectId())) {
					inhibitedDescItemIds.add(descItem.getItemId());
				}
			}
		}
		return inhibitedDescItemIds;
	}	

	/**
     * Update description item and return data (nová).
     * 
     * Method is called from WebSocket Controller
     *
     * @param fundVersionId
     * @param nodeId
     * @param nodeVersion
     * @param nodeItem
     * @param createVersion
     */
	@Transactional
	@AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
	public void updateDescItem(@AuthParam(type = AuthParam.Type.FUND_VERSION) int fundVersionId,
							   @AuthParam(type = AuthParam.Type.NODE) final Integer nodeId,
							   int nodeVersion, final NodeItem nodeItem, boolean createVersion,
							   StompHeaderAccessor requestHeaders) {

		ArrDescItem descItemUpdated = descriptionItemService.updateDescriptionItem(nodeItem, nodeItem.getNodeVersion(), nodeItem.getNodeId(), fundVersionId, createVersion, false);

		// odeslání dat zpět
		wsStompService.sendReceiptAfterCommit(createItemDataResult(descItemUpdated), requestHeaders);
	}

	@Deprecated
	@Transactional
	@AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
	public void updateDescItem(@AuthParam(type = AuthParam.Type.FUND_VERSION) int fundVersionId,
							   @AuthParam(type = AuthParam.Type.NODE) final Integer nodeId,
							   int nodeVersion, ArrItemVO descItemVO, boolean createVersion,
							   StompHeaderAccessor requestHeaders) {
		ArrFundVersion version = arrangementService.getFundVersion(fundVersionId);
		updateDescItem(version, nodeId, nodeVersion, descItemVO, createVersion, requestHeaders);
	}

	/**
	 * Hromadná úprava hodnot JP (nová).
	 *
	 * Funkce je volána z UI a respektuje read-only u prvků popisu
	 *
	 * @param fundVersionId  identifikátor verze AS
	 * @param nodeId         identifikátor uzlu
	 * @param nodeVersion    verze uzlu
	 * @param nodeUpdateItems      seznam
	 * @param requestHeaders reqh
	 */
	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
	public void updateDescItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
								@AuthParam(type = AuthParam.Type.NODE) final Integer nodeId,
								final Integer nodeVersion,
								final NodeUpdateItem[] nodeUpdateItems,
								@Nullable final StompHeaderAccessor requestHeaders) {

		ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
		ArrNode node = arrangementService.getNode(nodeId);

	    List<ArrDescItem> createItems = new ArrayList<>();
	    List<ArrDescItem> updateItems = new ArrayList<>();
	    List<ArrDescItem> deleteItems = new ArrayList<>();

	    for (NodeUpdateItem nodeItem : nodeUpdateItems) {
	        ArrDescItem descItem = factoryDo.createDescItem(nodeItem.getItem());
	        switch (nodeItem.getUpdateOp()) {
	        case CREATE:
	            createItems.add(descItem);
	            break;
	        case UPDATE:
	            updateItems.add(descItem);
	            break;
	        case DELETE:
	            deleteItems.add(descItem);
	            break;
	        }
	    }

		List<ArrDescItem> arrDescItems = updateDescItems(fundVersion, node, nodeVersion, createItems, updateItems, deleteItems);

		if (requestHeaders != null) {
			List<ItemDataResult> results = arrDescItems.stream().map(this::createItemDataResult).toList();

			// odeslání dat zpět
			wsStompService.sendReceiptAfterCommit(results, requestHeaders);
		}
	}

	/**
	 * Hromadná úprava hodnot JP.
	 *
	 * Funkce je volána z UI a respektuje read-only u prvků popisu
	 *
	 * @param fundVersionId  identifikátor verze AS
	 * @param params         parametry pro úpravu
	 * @param requestHeaders reqh
	 */
	@Deprecated
	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
	public void updateDescItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
								@AuthParam(type = AuthParam.Type.NODE) final Integer nodeId,
								final Integer nodeVersion,
								final UpdateDescItemsParam params,
								@Nullable final StompHeaderAccessor requestHeaders) {

		ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
		ArrNode node = arrangementService.getNode(nodeId);

		final StaticDataProvider dataProvider = this.staticData.getData();
		List<ArrDescItem> createItems = params.getCreateItemVOs().stream().map(itemVO -> factoryDo.createDescItem(dataProvider, itemVO)).collect(Collectors.toList());
		List<ArrDescItem> updateItems = params.getUpdateItemVOs().stream().map(itemVO -> factoryDo.createDescItem(dataProvider, itemVO)).collect(Collectors.toList());
		List<ArrDescItem> deleteItems = params.getDeleteItemVOs().stream().map(itemVO -> factoryDo.createDescItem(dataProvider, itemVO)).collect(Collectors.toList());

		List<ArrDescItem> arrDescItems = updateDescItems(fundVersion, node, nodeVersion, createItems, updateItems, deleteItems);

		if (requestHeaders != null) {
			List<UpdateItemResult> results = new ArrayList<>();

			// prepare form data
			List<RulItemTypeExt> itemTypes = ruleService.getDescriptionItemTypes(fundVersion, node);

            RuleSet rs = dataProvider.getRuleSetById(fundVersion.getRuleSetId());
			List<ItemTypeLiteVO> itemTypesVO = factoryVo.createItemTypes(rs.getCode(), fundVersion.getFundId(), itemTypes);

			LevelTreeCacheService.Node simpleNode = levelTreeCache.getSimpleNode(nodeId, fundVersion);
			for (ArrDescItem descItem : arrDescItems) {
				ArrItemVO descItemVo = factoryVo.createItem(descItem);
				results.add(new UpdateItemResult(descItem, descItemVo, itemTypesVO, simpleNode));
			}

			// odeslání dat zpět
			wsStompService.sendReceiptAfterCommit(results, requestHeaders);
		}
	}

	/**
	 * Hromadná úprava prvků popisu
	 *
	 * Funkce je volána z UI a respektuje read-only u prvků popisu
	 *
	 * @param fundVersion
	 * @param node
	 * @param nodeVersion
	 * @param createItems
	 * @param updateItems
	 * @param deleteItems
	 * @return
	 */
	private List<ArrDescItem> updateDescItems(final ArrFundVersion fundVersion,
                                              ArrNode node,
											  final Integer nodeVersion,
											  final List<ArrDescItem> createItems,
											  final List<ArrDescItem> updateItems,
											  final List<ArrDescItem> deleteItems) {
	    // urceni typu zmeny
        Type changeType = ArrChange.Type.BATCH_CHANGE_DESC_ITEM;
        if (CollectionUtils.isNotEmpty(createItems)) {
            if (CollectionUtils.isEmpty(deleteItems) && CollectionUtils.isEmpty(updateItems)) {
                changeType = Type.ADD_DESC_ITEM;
            }
        } else if (CollectionUtils.isNotEmpty(updateItems)) {
            if (CollectionUtils.isEmpty(deleteItems)) {
                changeType = Type.UPDATE_DESC_ITEM;
            }
        } else if(CollectionUtils.isNotEmpty(deleteItems)) {
            changeType = Type.DELETE_DESC_ITEM;
        }

        ArrChange change = arrangementInternalService.createChange(changeType, node);

		if (!node.getFundId().equals(fundVersion.getFundId())) {
			throw new SystemException("Nesedí verze JP s AS", ArrangementCode.INVALID_VERSION);
		}

		// uložení uzlu (kontrola optimistických zámků)
		node.setVersion(nodeVersion);
        node = descriptionItemService.saveNode(node, change);

		List<ArrDescItem> result = new ArrayList<>();

        MultipleItemChangeContext changeContext = descriptionItemService.createChangeContext(fundVersion.getFundVersionId());

		if (CollectionUtils.isNotEmpty(deleteItems)) {
            result.addAll(descriptionItemService.deleteDescriptionItems(deleteItems, fundVersion, change, true, false, changeContext));
		}

		if (CollectionUtils.isNotEmpty(updateItems)) {
            for (ArrDescItem updateDescItem : updateItems) {
                ArrDescItem updatedItem = descriptionItemService.updateValueAsNewVersion(fundVersion, change, updateDescItem, changeContext, false);
                result.add(updatedItem);
            }
		}

		if (CollectionUtils.isNotEmpty(createItems)) {
            for (ArrDescItem descItem : createItems) {
                ArrDescItem createdItem = descriptionItemService.createDescriptionItemInBatch(descItem, node, fundVersion, change, changeContext);
                result.add(createdItem);
            }
		}

        // ulozeni do db
        changeContext.flush();

		return result;
	}

	/**
     * Update description item and return form data
     *
     * Method is called from WebSocket Controller
     *
     * @param fundVersion
     * @param nodeVersion
     * @param descItemVO
     * @param createVersion
     */
	@Deprecated
	@Transactional
	@AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_NODE})
	public void updateDescItem(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
							   @AuthParam(type = AuthParam.Type.NODE) int nodeId,
							   int nodeVersion,
							   ArrItemVO descItemVO,
							   boolean createVersion,
							   StompHeaderAccessor requestHeaders) {

        // There is list of actions after transaction
        // - send response to client (highest priority)
        // - send node change notification
        // - post node validation request - after above notifications are submitted

		// alternative way of authorization - not finished
		/*
		userService.authorizeRequest(
		        AuthorizationRequest
		                .hasPermission(UsrPermission.Permission.ADMIN)
		                .or(UsrPermission.Permission.FUND_ARR_ALL)
		                .or(UsrPermission.Permission.FUND_ARR, fundVersion)
				);
				*/

		var sdp = staticData.getData();
		ArrDescItem descItem = factoryDo.createDescItem(sdp, descItemVO);

		// store updated value
		ArrDescItem descItemUpdated = descriptionItemService
		        .updateDescriptionItem(descItem, nodeVersion, nodeId, fundVersion.getFundVersionId(), createVersion, false);

		// prepare form data
		List<RulItemTypeExt> itemTypes = ruleService.getDescriptionItemTypes(fundVersion, descItemUpdated.getNode());

		StaticDataProvider dataProvider = this.staticData.getData();
        RuleSet rs = dataProvider.getRuleSetById(fundVersion.getRuleSetId());
		List<ItemTypeLiteVO> itemTypesVO = factoryVo.createItemTypes(rs.getCode(), fundVersion.getFundId(), itemTypes);

		ArrItemVO descItemVo = factoryVo.createItem(descItemUpdated);
		LevelTreeCacheService.Node node = levelTreeCache.getSimpleNode(descItemUpdated.getNodeId(), fundVersion);
		UpdateItemResult updateResult = new UpdateItemResult(descItemUpdated, descItemVo, itemTypesVO, node);

		// odeslání dat zpět
		wsStompService.sendReceiptAfterCommit(updateResult, requestHeaders);
	}

	// TODO: Refactorize return value to contain nodeId instead of parent
	@Deprecated
	public DescItemResult updateDescItem(int fundVersionId, int nodeId, int nodeVersion, ArrItemVO descItemVO, boolean createNewVersion) {

		var sdp = staticData.getData();
		ArrDescItem descItem = factoryDo.createDescItem(sdp, descItemVO);

		ArrDescItem descItemUpdated = descriptionItemService
				.updateDescriptionItem(descItem, nodeVersion, nodeId, fundVersionId, createNewVersion, false);

		DescItemResult descItemResult = new DescItemResult();
		descItemResult.setItem(factoryVo.createItem(descItemUpdated));
		descItemResult.setParent(ArrNodeVO.valueOf(descItemUpdated.getNode()));

		return descItemResult;
	}

	public ItemDataResult createItemDataResult(final ArrDescItem descItem) {
		ArrNode node = descItem.getNode();
		ItemDataResult itemDataResult = new ItemDataResult();
		itemDataResult.setItem(factoryVo.createNodeItem(descItem));
		itemDataResult.setParent(new NodeBase(node.getNodeId(), node.getVersion(), node.getUuid()));

		return itemDataResult;
	}
}
