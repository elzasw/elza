package cz.tacr.elza.service;

import static cz.tacr.elza.repository.ExceptionThrow.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import cz.tacr.elza.websocket.service.WebScoketStompService;

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

	private final LevelRepository levelRepository;

	private final LevelTreeCacheService levelTreeCache;

	private final NodeRepository nodeRepository;

	private final ClientFactoryDO factoryDo;

	private final ClientFactoryVO factoryVo;

	private final WebScoketStompService wsStompService;

	private final NodeCacheService nodeCacheService;

	private final ArrangementService arrangementService;

	private final ArrangementInternalService arrangementInternalService;

    private final UserService userService;

	public ArrangementFormService(StaticDataService staticData,
								  DescriptionItemServiceInternal arrangementInternal,
								  DescriptionItemService descriptionItemService,
								  LevelRepository levelRepository,
								  LevelTreeCacheService levelTreeCache,
								  UserService userService,
								  RuleService ruleService,
								  WebScoketStompService wsStompService,
								  ClientFactoryVO factoryVo,
								  ClientFactoryDO factoryDo,
								  NodeCacheService nodeCache,
								  FundVersionRepository fundVersionRepository,
								  NodeRepository nodeRepository, final ArrangementService arrangementService,
								  final ArrangementInternalService arrangementInternalService) {
		this.staticData = staticData;
		this.arrangementInternal = arrangementInternal;
		this.descriptionItemService = descriptionItemService;
		this.levelTreeCache = levelTreeCache;
		this.ruleService = ruleService;
		this.levelRepository = levelRepository;
		this.nodeRepository = nodeRepository;
		this.factoryDo = factoryDo;
		this.factoryVo = factoryVo;
		this.nodeCacheService = nodeCache;
		this.wsStompService = wsStompService;
		this.arrangementService = arrangementService;
		this.arrangementInternalService = arrangementInternalService;
        this.userService = userService;
	}

	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public DescFormDataNewVO getNodeFormData(@AuthParam(type = AuthParam.Type.FUND_VERSION) Integer versionId,
	        Integer nodeId) {
		ArrFundVersion version = arrangementService.getFundVersion(versionId);
		return getNodeFormData(version, nodeId);
	}

	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public DescFormDataNewVO getNodeFormData(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion version, Integer nodeId) {

		// získat seznam rodičovských nodů
		List<ArrLevel> levels = levelRepository.findAllParentsByNodeId(nodeId, null, true);
		List<Integer> parentNodeIds = levels.stream().map(i -> i.getNodeId()).toList();

		ArrChange lockChange = version.getLockChange();
		ArrNode node;
		List<ArrDescItem> descItems;
		Set<Integer> inhibitedDescItemIds;
		Set<Integer> inhibitedNodeDescItemIds;
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
		}

		List<RulItemTypeExt> itemTypes;
		List<Integer> itemTypeIdsWithInheritance;
		try {
			itemTypes = ruleService.getDescriptionItemTypes(version, node);
			itemTypeIdsWithInheritance = itemTypes.stream()
					.filter(i -> i.isInheritance())
					.map(i -> i.getItemTypeId())
					.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Chyba v pravidlech", e);
			throw new BusinessException("Chyba v pravidlech", e, BaseCode.SYSTEM_ERROR);
		}

		if (lockChange == null) {
			descItems = restoredNode.getDescItems();
			// v uzlu, kde je dědičnost potlačena, stále zobrazujeme zděděné záznamy
			inhibitedDescItemIds = new HashSet<>();
			// sbíráme id záznamy (descItemId) s potlačenou dědičností od nadřazených uzlů
			parentRestoredNodes.forEach(n ->
				inhibitedDescItemIds.addAll(n.getInhibitedItems().stream().map(i -> i.getDescItemId()).collect(Collectors.toSet())));
			// sbíráme všechny descItems s povolenou dědičností z nadřazených uzlů
			parentsDescItems = parentRestoredNodes.stream()
					.flatMap(i -> i.getDescItems().stream())
					.filter(i -> itemTypeIdsWithInheritance.contains(i.getDescItemTypeId()))
					.toList();
			// seznam descItemId s potlačenou dědičností pro aktuální uzel
			inhibitedNodeDescItemIds = restoredNode.getInhibitedItems().stream().map(i -> i.getDescItemId()).collect(Collectors.toSet());
		} else {
			descItems = arrangementInternal.getDescItems(lockChange, node);
			inhibitedDescItemIds = arrangementInternal.getInhibitedDescItemIds(lockChange, parentNodeIds);
			parentsDescItems = descriptionItemService.findByNodeIdsAndDeleteChangeIsNull(parentNodeIds, itemTypeIdsWithInheritance);
			inhibitedNodeDescItemIds = arrangementInternal.getInhibitedDescItemIds(lockChange, List.of(nodeId));
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
		List<ArrItemVO> descItemsVOs = factoryVo.createItems(nodeId, descItems, inhibitedNodeDescItemIds);
		List<ItemTypeLiteVO> itemTypeLites = factoryVo.createItemTypes(ruleCode, fundId, itemTypes);

        boolean arrPerm = userService.hasFullArrPerm(version.getFundId());
		if (!arrPerm) {
			Map<Integer, Boolean> permNodeIdMap = levelTreeCache.calcPermNodeIdMap(version, Collections.singleton(nodeId));
			arrPerm = permNodeIdMap.get(nodeId);
		}
		return new DescFormDataNewVO(nodeVO, descItemsVOs, itemTypeLites, arrPerm);
	}

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
	 * Hromadná úprava hodnot JP.
	 *
	 * Funkce je volána z UI a respektuje read-only u prvků popisu
	 *
	 * @param fundVersionId  identifikátor verze AS
	 * @param params         parametry pro úpravu
	 * @param requestHeaders reqh
	 */
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
		List<ArrDescItem> createItems = params.getCreateItemVOs().stream().map(itemVO -> convertDescItem(dataProvider, itemVO)).collect(Collectors.toList());
		List<ArrDescItem> updateItems = params.getUpdateItemVOs().stream().map(itemVO -> convertDescItem(dataProvider, itemVO)).collect(Collectors.toList());
		List<ArrDescItem> deleteItems = params.getDeleteItemVOs().stream().map(itemVO -> convertDescItem(dataProvider, itemVO)).collect(Collectors.toList());

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

			// Odeslání dat zpět
			wsStompService.sendReceiptAfterCommit(results, requestHeaders);
		}
	}

	private ArrDescItem convertDescItem(final StaticDataProvider sdp, final ArrItemVO itemVO) {
		ArrDescItem descItem = factoryDo.createDescItem(itemVO);
		descItem.setItemType(sdp.getItemTypeById(itemVO.getItemTypeId()).getEntity());
		return descItem;
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

        MultipleItemChangeContext changeContext = descriptionItemService.createChangeContext(fundVersion
                .getFundVersionId());

		if (CollectionUtils.isNotEmpty(deleteItems)) {
            result.addAll(descriptionItemService.deleteDescriptionItems(deleteItems, fundVersion, change, true,
                                                                        false,
                                                                        changeContext));
		}

		if (CollectionUtils.isNotEmpty(updateItems)) {
            for (ArrDescItem updateDescItem : updateItems) {
                ArrDescItem updatedItem = descriptionItemService.updateValueAsNewVersion(fundVersion, change,
                                                                                         updateDescItem, changeContext,
                                                                                         false);
                result.add(updatedItem);
            }
		}

		if (CollectionUtils.isNotEmpty(createItems)) {
            for (ArrDescItem descItem : createItems) {
                ArrDescItem createdItem = descriptionItemService.createDescriptionItemInBatch(descItem, node,
                                                                                              fundVersion, change,
                                                                                              changeContext);
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


		ArrDescItem descItem = factoryDo.createDescItem(descItemVO);

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

		// Odeslání dat zpět
		wsStompService.sendReceiptAfterCommit(updateResult, requestHeaders);
	}

	// TODO: Refactorize return value to contain nodeId instead of parent
	public DescItemResult updateDescItem(int fundVersionId, int nodeId, int nodeVersion, ArrItemVO descItemVO, boolean createNewVersion) {

		ArrDescItem descItem = factoryDo.createDescItem(descItemVO);

		ArrDescItem descItemUpdated = descriptionItemService
				.updateDescriptionItem(descItem, nodeVersion, nodeId, fundVersionId, createNewVersion, false);

		DescItemResult descItemResult = new DescItemResult();
		descItemResult.setItem(factoryVo.createItem(descItemUpdated));
		descItemResult.setParent(ArrNodeVO.valueOf(descItemUpdated.getNode()));

		return descItemResult;
	}

}
