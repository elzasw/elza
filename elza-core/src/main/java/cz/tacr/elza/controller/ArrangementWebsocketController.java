package cz.tacr.elza.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.ArrangementFormService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.websocket.WebSocketAwareController;
import cz.tacr.elza.websocket.WebsocketCallback;

/**
 * Kontroler pro zpracování websocket požadavků pro některé kritické modifikace v pořádíní.
 * Jedná se o modifikace, které vyžadují seriové zpracování.
 *
 */
@Controller
@WebSocketAwareController
public class ArrangementWebsocketController {
	@Autowired
	ArrangementFormService arrangementFormService;

    @Autowired
    private ClientFactoryDO factoryDO;
    @Autowired
    private WebsocketCallback websocketCallback;
    @Autowired
    private DescriptionItemService descriptionItemService;
    @Autowired
    private ClientFactoryVO factoryVo;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private ArrMoveLevelService moveLevelService;
    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

	@MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}")
	public void updateDescItem(
	        @Payload final ArrItemVO descItemVO,
	        @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
	        @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
	        @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
	        final SimpMessageHeaderAccessor headerAccessor) {

		Validate.notNull(fundVersionId);
		Validate.notNull(nodeVersion);
		Validate.notNull(descItemVO);

		UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) headerAccessor
		        .getHeader("simpUser");
		SecurityContext sc = new SecurityContextImpl();
		sc.setAuthentication(token);
		SecurityContextHolder.setContext(sc);

		arrangementFormService.updateDescItem(fundVersionId, nodeVersion, descItemVO,
		        BooleanUtils.isNotFalse(createNewVersion),
		        headerAccessor);
	}

    /**
     * Aktualizace hodnoty atributu.
     *
     * @param descItemVO       hodnota atributu
     * @param fundVersionId    identfikátor verze AP
     * @param nodeVersion      verze JP
     * @param createNewVersion vytvořit novou verzi?
     */
    @Transactional
	@MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeVersion}/updateOld/{createNewVersion}")
	public void updateDescItemOld(
            @Payload final ArrItemVO descItemVO,
            @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
            @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
            @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
            final SimpMessageHeaderAccessor headerAccessor
    ) {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) headerAccessor.getHeader("simpUser");
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(token);
        SecurityContextHolder.setContext(sc);

		Validate.notNull(descItemVO, "Hodnota atributu musí být vyplněna");
		Validate.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
		Validate.notNull(nodeVersion, "Nebyla vyplněna verze JP");
		Validate.notNull(createNewVersion, "Vytvořit novou verzi musí být vyplněno");

        ArrDescItem descItem = factoryDO.createDescItem(descItemVO);

        ArrDescItem descItemUpdated = descriptionItemService
                .updateDescriptionItem(descItem, nodeVersion, fundVersionId, createNewVersion);

        ArrangementController.DescItemResult descItemResult = new ArrangementController.DescItemResult();
        descItemResult.setItem(factoryVo.createDescItem(descItemUpdated));
        descItemResult.setParent(factoryVo.createArrNode(descItemUpdated.getNode()));

        // Odeslání dat zpět
		websocketCallback.sendAfterCommit(descItemResult, headerAccessor);
    }

    /**
     * Přidání uzlu do stromu.
     *
     * @param addLevelParam vstupní parametry
     * @return nový přidaný uzel
     */
    @Transactional
    @MessageMapping("/arrangement/levels/add")
    public void addLevel(
            @Payload final ArrangementController.AddLevelParam addLevelParam,
            final SimpMessageHeaderAccessor headerAccessor
    ) {
        Assert.notNull(addLevelParam, "Parametry musí být vyplněny");
        Assert.notNull(addLevelParam.getVersionId(), "Nebyla vyplněn identifikátor verze AS");

        Assert.notNull(addLevelParam.getDirection(), "Směr musí být vyplněn");

        ArrFundVersion version = fundVersionRepository.findOne(addLevelParam.getVersionId());

        ArrNode staticNode = factoryDO.createNode(addLevelParam.getStaticNode());
        ArrNode staticParentNode = addLevelParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(addLevelParam.getStaticNodeParent());

        Set<RulItemType> descItemCopyTypes = new HashSet<>();
        if (CollectionUtils.isNotEmpty(addLevelParam.getDescItemCopyTypes())) {
            descItemCopyTypes.addAll(itemTypeRepository.findAll(addLevelParam.getDescItemCopyTypes()));
        }


        ArrLevel newLevel = moveLevelService.addNewLevel(version, staticNode, staticParentNode,
                addLevelParam.getDirection(), addLevelParam.getScenarioName(),
                descItemCopyTypes);

        Collection<TreeNodeClient> nodeClients = levelTreeCacheService
                .getNodesByIds(Collections.singletonList(newLevel.getNodeParent().getNodeId()), version.getFundVersionId());
        Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(factoryVo.createArrNode(newLevel.getNode()), nodeClients.iterator().next());

        // Odeslání dat zpět
		websocketCallback.sendAfterCommit(result, headerAccessor);
    }

    /**
     * Smazání uzlu.
     *
     * @param nodeParam vstupní parametry pro smazání
     */
    @Transactional
    @MessageMapping("/arrangement/levels/delete")
    public void deleteLevel(
            @Payload final ArrangementController.NodeParam nodeParam,
            final SimpMessageHeaderAccessor headerAccessor
    ) {
        Assert.notNull(nodeParam, "Parametry JP musí být vyplněny");
        Assert.notNull(nodeParam.getVersionId(), "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(nodeParam.getStaticNode(), "Nebyla zvolena referenční JP");

        ArrNode deleteNode = factoryDO.createNode(nodeParam.getStaticNode());
        ArrNode deleteParent = nodeParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(nodeParam.getStaticNodeParent());

        ArrFundVersion version = fundVersionRepository.findOne(nodeParam.getVersionId());

        ArrLevel deleteLevel = moveLevelService.deleteLevel(version, deleteNode, deleteParent);

        Collection<TreeNodeClient> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(deleteLevel.getNodeParent().getNodeId()),
                        version.getFundVersionId());
        Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(factoryVo.createArrNode(deleteLevel.getNode()), nodeClients.iterator().next());

        // Odeslání dat zpět
		websocketCallback.sendAfterCommit(result, headerAccessor);
    }

    // Pokus o jiný způsob vracení dat - problém s podíláním receipt id - necháno z důvodu připadného budoucího rozchození
//    @Publisher(channel="clientOutboundChannel")
//    @Transactional
//    @MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeVersion}/update2/{createNewVersion}")
//    public ArrangementController.DescItemResult updateDescItem2(
//            @Payload final ArrItemVO descItemVO,
//            @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
//            @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
//            @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
//            SimpMessageHeaderAccessor headerAccessor) {
//
//        SecurityContext sc = new SecurityContextImpl();
//        sc.setAuthentication(token);
//        SecurityContextHolder.setContext(sc);
//
//        final List<String> receipt = headerAccessor.getNativeHeader("receipt");
//        final String receiptId = receipt == null || receipt.isEmpty() ? null : receipt.get(0);
//
//        Assert.notNull(descItemVO, "Hodnota atributu musí být vyplněna");
//        Assert.notNull(fundVersionId, "Nebyla vyplněn identifikátor verze AS");
//        Assert.notNull(nodeVersion, "Nebyla vyplněna verze JP");
//        Assert.notNull(createNewVersion, "Vytvořit novou verzi musí být vyplněno");
//
//        ArrDescItem descItem = factoryDO.createDescItem(descItemVO);
//
//        ArrDescItem descItemUpdated = descriptionItemService
//                .updateDescriptionItem(descItem, nodeVersion, fundVersionId, createNewVersion);
//
//        ArrangementController.DescItemResult descItemResult = new ArrangementController.DescItemResult();
//        descItemResult.setItem(factoryVo.createDescItem(descItemUpdated));
//        descItemResult.setParent(factoryVo.createArrNode(descItemUpdated.getNode()));
//
//        if (false) {
//            throw new RuntimeException("xxxxx");
//        }
//
//        // Odeslání dat zpět
////        Map sendHeader = new HashMap();
////        sendHeader.put("receipt-id", receiptId);
////        messagingTemplate.convertAndSend("/topic/api/changes", descItemResult, sendHeader);
//
//        return descItemResult;
//    }
}
