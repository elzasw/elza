package cz.tacr.elza.service;

import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.controller.ArrangementController.DescFormDataNewVO;
import cz.tacr.elza.controller.ArrangementController.DescItemResult;
import cz.tacr.elza.controller.arrangement.UpdateItemResult;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemGroupVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ItemTypeGroupVO;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
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

	private final ArrangementServiceInternal arrangementInternal;

	private final DescriptionItemService descriptionItemService;

	private final RuleService ruleService;

	private final FundVersionRepository fundVersionRepository;

	private final LevelTreeCacheService levelTreeCache;

	private final NodeRepository nodeRepository;

	private final ClientFactoryDO factoryDo;

	private final ClientFactoryVO factoryVo;

	private final WebScoketStompService wsStompService;

	private final NodeCacheService nodeCache;

	public ArrangementFormService(StaticDataService staticData,
	        ArrangementServiceInternal arrangementInternal,
	        DescriptionItemService descriptionItemService,
	        LevelTreeCacheService levelTreeCache,
	        UserService userService,
	        RuleService ruleService,
	        WebScoketStompService wsStompService,
	        ClientFactoryVO factoryVo,
	        ClientFactoryDO factoryDo,
	        NodeCacheService nodeCache,
	        FundVersionRepository fundVersionRepository,
	        NodeRepository nodeRepository) {
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
		List<ItemGroupVO> descItemGroupsVO = factoryVo.createItemGroupsNew(ruleCode, fundId, descItems);
		List<ItemTypeGroupVO> descItemTypeGroupsVO = factoryVo
		        .createItemTypeGroupsNew(ruleCode, fundId, itemTypes);
		return new DescFormDataNewVO(nodeVO, descItemGroupsVO, descItemTypeGroupsVO);
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
	 * Update description item and return form data
	 *
	 * @param fundVersion
	 * @param nodeVersion
	 * @param descItemVO
	 * @param createVersion
	 * @param headerAccessor
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
		RuleSystem rs = dataProvider.getRuleSystems().getByRuleSetId(fundVersion.getRuleSet().getRuleSetId());
		//
		List<ItemTypeGroupVO> descItemTypeGroupsVO = factoryVo
		        .createItemTypeGroupsNew(rs.getRuleSet().getCode(), fundVersion.getFundId(), itemTypes);

		ArrItemVO descItemVo = factoryVo.createDescItem(descItemUpdated);

		// TODO: use better functions, we just need a descriptions
		List<TreeNodeClient> tncList = levelTreeCache.getNodesByIds(Collections.singleton(descItemUpdated.getNodeId()),
		        fundVersion.getFundId());
		TreeNodeClient tnc = tncList.get(0);

		UpdateItemResult updateResult = new UpdateItemResult(descItemUpdated, descItemVo, descItemTypeGroupsVO, tnc);

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
		descItemResult.setItem(factoryVo.createDescItem(descItemUpdated));
		descItemResult.setParent(ArrNodeVO.valueOf(descItemUpdated.getNode()));

		return descItemResult;
	}

}
