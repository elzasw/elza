package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

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
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
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

	private final FundVersionRepository fundVersionRepository;

	private final LevelTreeCacheService levelTreeCache;

	private final NodeRepository nodeRepository;

	private final ClientFactoryDO factoryDo;

	private final ClientFactoryVO factoryVo;

	private final WebScoketStompService wsStompService;

	private final NodeCacheService nodeCache;

	private final ArrangementService arrangementService;

	public ArrangementFormService(StaticDataService staticData,
								  DescriptionItemServiceInternal arrangementInternal,
								  DescriptionItemService descriptionItemService,
								  LevelTreeCacheService levelTreeCache,
								  UserService userService,
								  RuleService ruleService,
								  WebScoketStompService wsStompService,
								  ClientFactoryVO factoryVo,
								  ClientFactoryDO factoryDo,
								  NodeCacheService nodeCache,
								  FundVersionRepository fundVersionRepository,
								  NodeRepository nodeRepository, final ArrangementService arrangementService) {
		this.staticData = staticData;
		this.arrangementInternal = arrangementInternal;
		this.descriptionItemService = descriptionItemService;
		this.levelTreeCache = levelTreeCache;
		this.ruleService = ruleService;
		this.fundVersionRepository = fundVersionRepository;
		this.nodeRepository = nodeRepository;
		this.factoryDo = factoryDo;
		this.factoryVo = factoryVo;
		this.nodeCache = nodeCache;
		this.wsStompService = wsStompService;
		this.arrangementService = arrangementService;
	}

	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public DescFormDataNewVO getNodeFormData(@AuthParam(type = AuthParam.Type.FUND_VERSION) Integer versionId,
	        Integer nodeId) {

		ArrFundVersion version = fundVersionRepository.findOne(versionId);
		if (version == null) {
			throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + versionId,
			        ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", versionId);
		}

		return getNodeFormData(version, nodeId);
	}

	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_RD_ALL, UsrPermission.Permission.FUND_RD })
	public DescFormDataNewVO getNodeFormData(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion version,
	        Integer nodeId) {

		ArrChange lockChange = version.getLockChange();
		ArrNode node;
		List<ArrDescItem> descItems;
		if (lockChange == null) {
			// read node from cache
			RestoredNode restoredNode = nodeCache.getNode(nodeId);
			if (restoredNode == null) {
				throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + nodeId, ArrangementCode.NODE_NOT_FOUND)
				        .set("id", nodeId);
			}
			node = restoredNode.getNode();
			descItems = restoredNode.getDescItems();
		} else {
			// check if node exists
			node = nodeRepository.findOne(nodeId);
			if (node == null) {
				throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + nodeId, ArrangementCode.NODE_NOT_FOUND)
				        .set("id", nodeId);
			}
			descItems = arrangementInternal.getDescItems(lockChange, node);
		}

		List<RulItemTypeExt> itemTypes;
		try {
			itemTypes = ruleService.getDescriptionItemTypes(version, node);
		} catch (Exception e) {
			logger.error("Chyba v pravidlech", e);
			throw new BusinessException("Chyba v pravidlech", e, BaseCode.SYSTEM_ERROR);
		}

		Integer fundId = version.getFund().getFundId();
		String ruleCode = version.getRuleSet().getCode();

		ArrNodeVO nodeVO = ArrNodeVO.valueOf(node);
		List<ArrItemVO> descItemsVOs = factoryVo.createItems(descItems);
		List<ItemTypeLiteVO> itemTypeLites = factoryVo.createItemTypes(ruleCode, fundId, itemTypes);
		return new DescFormDataNewVO(nodeVO, descItemsVOs, itemTypeLites);
	}

	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR })
	public void updateDescItem(@AuthParam(type = AuthParam.Type.FUND_VERSION) int fundVersionId,
	        int nodeVersion, ArrItemVO descItemVO, boolean createVersion,
	        StompHeaderAccessor requestHeaders) {
		ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);
		if (version == null) {
			throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId,
			        ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
		}
		updateDescItem(version, nodeVersion, descItemVO, createVersion, requestHeaders);
	}

	/**
	 * Hromadná úprava hodnot JP.
	 *
	 * @param fundVersionId  identifikátor verze AS
	 * @param params         parametry pro úpravu
	 * @param requestHeaders reqh
	 */
	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR })
	public void updateDescItems(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer fundVersionId,
								final UpdateDescItemsParam params,
								@Nullable final StompHeaderAccessor requestHeaders) {
		ArrFundVersion fundVersion = fundVersionRepository.findOne(fundVersionId);
		if (fundVersion == null) {
			throw new ObjectNotFoundException("Nebyla nalezena verze AS s ID=" + fundVersionId,
					ArrangementCode.FUND_VERSION_NOT_FOUND).set("id", fundVersionId);
		}
		ArrNode node = nodeRepository.findOne(params.getNodeId());
		if (node == null) {
			throw new ObjectNotFoundException("Nebyla nalezena JP s ID=" + params.getNodeId(),
					ArrangementCode.NODE_NOT_FOUND).set("id", params.getNodeId());
		}
		final StaticDataProvider dataProvider = this.staticData.getData();
		List<ArrDescItem> createItems = params.getCreateItemVOs().stream().map(itemVO -> convertDescItem(dataProvider, itemVO)).collect(Collectors.toList());
		List<ArrDescItem> updateItems = params.getUpdateItemVOs().stream().map(itemVO -> convertDescItem(dataProvider, itemVO)).collect(Collectors.toList());
		List<ArrDescItem> deleteItems = params.getDeleteItemVOs().stream().map(itemVO -> convertDescItem(dataProvider, itemVO)).collect(Collectors.toList());

		List<ArrDescItem> arrDescItems = updateDescItems(fundVersion, node, params.getNodeVersion(), createItems, updateItems, deleteItems);

		if (requestHeaders != null) {
			List<UpdateItemResult> results = new ArrayList<>();

			// prepare form data
			List<RulItemTypeExt> itemTypes = ruleService.getDescriptionItemTypes(fundVersion, node);

			RulRuleSet rs = dataProvider.getRuleSetById(fundVersion.getRuleSetId());
			List<ItemTypeLiteVO> itemTypesVO = factoryVo.createItemTypes(rs.getCode(), fundVersion.getFundId(), itemTypes);

			LevelTreeCacheService.Node simpleNode = levelTreeCache.getSimpleNode(params.getNodeId(), fundVersion);
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

	private List<ArrDescItem> updateDescItems(final ArrFundVersion fundVersion,
											  final ArrNode node,
											  final Integer nodeVersion,
											  final List<ArrDescItem> createItems,
											  final List<ArrDescItem> updateItems,
											  final List<ArrDescItem> deleteItems) {

		ArrChange change = arrangementService.createChange(ArrChange.Type.BATCH_CHANGE_DESC_ITEM, node);

		if (!node.getFundId().equals(fundVersion.getFundId())) {
			throw new SystemException("Nesedí verze JP s AS", ArrangementCode.INVALID_VERSION);
		}

		// uložení uzlu (kontrola optimistických zámků)
		node.setVersion(nodeVersion);
		descriptionItemService.saveNode(node, change);

		List<ArrDescItem> result = new ArrayList<>();
		List<ArrDescItem> createdItems = null;
		List<ArrDescItem> updatedItems = null;
		List<ArrDescItem> deletedItems = null;

		if (CollectionUtils.isNotEmpty(deleteItems)) {
			deletedItems = descriptionItemService.deleteDescriptionItems(deleteItems, node, fundVersion, change);
			result.addAll(deletedItems);
		}

		if (CollectionUtils.isNotEmpty(updateItems)) {
			updatedItems = descriptionItemService.updateDescriptionItems(updateItems, fundVersion, change);
			result.addAll(updatedItems);
		}

		if (CollectionUtils.isNotEmpty(createItems)) {
			createdItems = descriptionItemService.createDescriptionItems(createItems, node, fundVersion, change);
			result.addAll(createdItems);
		}

		// validace uzlu
		ruleService.conformityInfo(fundVersion.getFundVersionId(), Collections.singletonList(node.getNodeId()),
				NodeTypeOperation.SAVE_DESC_ITEM, createdItems, updatedItems, deletedItems);

		return result;
	}

	/**
	 * Update description item and return form data
	 *
	 * @param fundVersion
	 * @param nodeVersion
	 * @param descItemVO
	 * @param createVersion
	 */
	@Transactional
	@AuthMethod(permission = { UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR })
	public void updateDescItem(@AuthParam(type = AuthParam.Type.FUND_VERSION) ArrFundVersion fundVersion,
	        int nodeVersion, ArrItemVO descItemVO, boolean createVersion,
	        StompHeaderAccessor requestHeaders) {

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
		        .updateDescriptionItem(descItem, nodeVersion, fundVersion.getFundVersionId(), createVersion);

		// prepare form data
		List<RulItemTypeExt> itemTypes = ruleService.getDescriptionItemTypes(fundVersion, descItemUpdated.getNode());

		StaticDataProvider dataProvider = this.staticData.getData();
		RulRuleSet rs = dataProvider.getRuleSetById(fundVersion.getRuleSetId());
		List<ItemTypeLiteVO> itemTypesVO = factoryVo.createItemTypes(rs.getCode(), fundVersion.getFundId(), itemTypes);

		ArrItemVO descItemVo = factoryVo.createItem(descItemUpdated);
		LevelTreeCacheService.Node node = levelTreeCache.getSimpleNode(descItemUpdated.getNodeId(), fundVersion);
		UpdateItemResult updateResult = new UpdateItemResult(descItemUpdated, descItemVo, itemTypesVO, node);

		// Odeslání dat zpět
		wsStompService.sendReceiptAfterCommit(updateResult, requestHeaders);
	}

	// TODO: Refactorize return value to contain nodeId instead of parent
	public DescItemResult updateDescItem(int fundVersionId, int nodeVersion, ArrItemVO descItemVO,
	        boolean createNewVersion) {

		ArrDescItem descItem = factoryDo.createDescItem(descItemVO);

		ArrDescItem descItemUpdated = descriptionItemService
		        .updateDescriptionItem(descItem, nodeVersion, fundVersionId, createNewVersion);

		DescItemResult descItemResult = new DescItemResult();
		descItemResult.setItem(factoryVo.createItem(descItemUpdated));
		descItemResult.setParent(ArrNodeVO.valueOf(descItemUpdated.getNode()));

		return descItemResult;
	}

}
