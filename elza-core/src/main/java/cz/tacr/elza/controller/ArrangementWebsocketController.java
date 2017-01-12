package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrMoveLevelService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.websocket.WebSocketAwareController;
import cz.tacr.elza.websocket.WebsocketCallback;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.transaction.Transactional;
import java.util.*;

import static org.geotools.kml.v22.KML.message;

/**
 * Kontroler pro zpracování websocket požadavků pro některé kritické modifikace v pořádíní.
 * Jedná se o modifikace, které vyžadují seriové zpracování.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 24.10.2016
 */
@Controller
@WebSocketAwareController
public class ArrangementWebsocketController {
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


    /**
     * Aktualizace hodnoty atributu.
     *
     * @param descItemVO       hodnota atributu
     * @param fundVersionId    identfikátor verze AP
     * @param nodeVersion      verze JP
     * @param createNewVersion vytvořit novou verzi?
     */
    @Transactional
    @MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}")
    public void updateDescItem(
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

        Assert.notNull(descItemVO);
        Assert.notNull(fundVersionId);
        Assert.notNull(nodeVersion);
        Assert.notNull(createNewVersion);

        ArrDescItem descItem = factoryDO.createDescItem(descItemVO);

        ArrDescItem descItemUpdated = descriptionItemService
                .updateDescriptionItem(descItem, nodeVersion, fundVersionId, createNewVersion);

        ArrangementController.DescItemResult descItemResult = new ArrangementController.DescItemResult();
        descItemResult.setItem(factoryVo.createDescItem(descItemUpdated));
        descItemResult.setParent(factoryVo.createArrNode(descItemUpdated.getNode()));

        // Odeslání dat zpět
        sendAfterCommit(descItemResult, headerAccessor);
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
        Assert.notNull(addLevelParam);
        Assert.notNull(addLevelParam.getVersionId());

        Assert.notNull(addLevelParam.getDirection());

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
                .getNodesByIds(Arrays.asList(newLevel.getNodeParent().getNodeId()), version.getFundVersionId());
        Assert.notEmpty(nodeClients);
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(factoryVo.createArrNode(newLevel.getNode()), nodeClients.iterator().next());

        // Odeslání dat zpět
        sendAfterCommit(result, headerAccessor);
    }

    /**
     * Poslání dat zpět až po provedení commitu transakce.
     * @param resultData data pro poslání
     * @param headerAccessor geader sccessor
     */
    private void sendAfterCommit(final Object resultData, final SimpMessageHeaderAccessor headerAccessor) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                // Odeslání dat zpět
                websocketCallback.send(resultData, headerAccessor);
            }
        });
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
        Assert.notNull(nodeParam);
        Assert.notNull(nodeParam.getVersionId());
        Assert.notNull(nodeParam.getStaticNode());

        ArrNode deleteNode = factoryDO.createNode(nodeParam.getStaticNode());
        ArrNode deleteParent = nodeParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(nodeParam.getStaticNodeParent());

        ArrFundVersion version = fundVersionRepository.findOne(nodeParam.getVersionId());

        ArrLevel deleteLevel = moveLevelService.deleteLevel(version, deleteNode, deleteParent);

        Collection<TreeNodeClient> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(deleteLevel.getNodeParent().getNodeId()),
                        version.getFundVersionId());
        Assert.notEmpty(nodeClients);
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(factoryVo.createArrNode(deleteLevel.getNode()), nodeClients.iterator().next());

        // Odeslání dat zpět
        sendAfterCommit(result, headerAccessor);
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
//        Assert.notNull(descItemVO);
//        Assert.notNull(fundVersionId);
//        Assert.notNull(nodeVersion);
//        Assert.notNull(createNewVersion);
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
