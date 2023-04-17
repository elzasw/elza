package cz.tacr.elza.controller;

import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrUpdateItemVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrangementFormService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.vo.UpdateDescItemsParam;
import cz.tacr.elza.websocket.WebSocketAwareController;
import cz.tacr.elza.websocket.service.WebScoketStompService;

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
    ArrangementService arrangementService;
    @Autowired
    private ClientFactoryDO factoryDO;
    @Autowired
    private WebScoketStompService webScoketStompService;
    @Autowired
    private FundVersionRepository fundVersionRepository;
    @Autowired
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private FundLevelService moveLevelService;
    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

	@MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/{createNewVersion}")
	public void updateDescItem(
	        @Payload final ArrItemVO descItemVO,
	        @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
	        @DestinationVariable(value = "nodeId") final Integer nodeId,
	        @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
	        @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
	        final StompHeaderAccessor requestHeaders) {

		Validate.notNull(fundVersionId);
		Validate.notNull(nodeId);
		Validate.notNull(nodeVersion);
		Validate.notNull(descItemVO);

        // authorize request as logged used
		UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) requestHeaders
		        .getHeader("simpUser");
		SecurityContext sc = new SecurityContextImpl();
		sc.setAuthentication(token);
		SecurityContextHolder.setContext(sc);

		arrangementFormService.updateDescItem(fundVersionId, nodeId, nodeVersion, descItemVO,
		        BooleanUtils.isNotFalse(createNewVersion),
		        requestHeaders);
	}

    @MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/bulk")
    public void updateDescItems(@Payload final ArrUpdateItemVO[] changeItems,
                                @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
                                @DestinationVariable(value = "nodeId") final Integer nodeId,
                                @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
                                final StompHeaderAccessor requestHeaders) {
        Validate.notEmpty(changeItems);
        Validate.notNull(nodeId);
        Validate.notNull(nodeVersion);
        Validate.notNull(fundVersionId);

        List<ArrItemVO> createItems = new ArrayList<>();
        List<ArrItemVO> updateItems = new ArrayList<>();
        List<ArrItemVO> deleteItems = new ArrayList<>();
        for (ArrUpdateItemVO changeItem : changeItems) {
            ArrItemVO item = changeItem.getItem();
            switch (changeItem.getUpdateOp()) {
                case CREATE:
                    createItems.add(item);
                    break;
                case DELETE:
                    deleteItems.add(item);
                    break;
                case UPDATE:
                    updateItems.add(item);
                    break;
                default:
                    throw new SystemException("Neimplementovaný typ operace: " + changeItem.getUpdateOp());
            }
        }

        UpdateDescItemsParam params = new UpdateDescItemsParam(
                createItems,
                updateItems,
                deleteItems);
        arrangementFormService.updateDescItems(fundVersionId, nodeId, nodeVersion, params, requestHeaders);
    }

    /**
     * Přidání uzlu do stromu.
     *
     * @param addLevelParam vstupní parametry
     * @param requestHeaders
     * @return nový přidaný uzel
     */
    @Transactional
    @MessageMapping("/arrangement/levels/add")
    public void addLevel(@Payload final AddLevelParam addLevelParam,
                         final StompHeaderAccessor requestHeaders) {

        Assert.notNull(addLevelParam, "Parametry musí být vyplněny");
        Integer versionId = addLevelParam.getVersionId();
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(addLevelParam.getDirection(), "Směr musí být vyplněn");

        ArrFundVersion version = fundVersionRepository.findById(versionId).orElseThrow(version(versionId));

        ArrNode staticNode = factoryDO.createNode(addLevelParam.getStaticNode());
        ArrNode staticParentNode = addLevelParam.getStaticNodeParent() == null ? 
                null : factoryDO.createNode(addLevelParam.getStaticNodeParent());

        Set<RulItemType> descItemCopyTypes = new HashSet<>();
        if (CollectionUtils.isNotEmpty(addLevelParam.getDescItemCopyTypes())) {
            descItemCopyTypes.addAll(itemTypeRepository.findAllById(addLevelParam.getDescItemCopyTypes()));
        }

        List<ArrLevel> newLevels = moveLevelService.addNewLevel(version, staticNode, staticParentNode,
                                                         addLevelParam.getDirection(), addLevelParam.getScenarioName(),
                                                         descItemCopyTypes, null, addLevelParam.getCount(), null);
        List<ArrNodeVO> nodes = new ArrayList<>(newLevels.size());
        Collection<TreeNodeVO> nodeClients = null;

        for (ArrLevel newLevel : newLevels) {
            if (CollectionUtils.isNotEmpty(addLevelParam.getCreateItems())) {
                UpdateDescItemsParam params = new UpdateDescItemsParam(
                        addLevelParam.getCreateItems(),
                        Collections.emptyList(),
                        Collections.emptyList());
                arrangementFormService.updateDescItems(version.getFundVersionId(), newLevel.getNodeId(), newLevel.getNode().getVersion(), params, null);
            }

            nodes.add(ArrNodeVO.newInstance(newLevel.getNode()));

            if (nodeClients == null) {
                nodeClients = levelTreeCacheService.getNodesByIds(Collections.singletonList(newLevel.getNodeParent().getNodeId()), version);
                Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
            }
        }

        final ArrangementController.NodesWithParent result = new ArrangementController.NodesWithParent(nodes, nodeClients.iterator().next());

        // Odeslání dat zpět
        webScoketStompService.sendReceiptAfterCommit(result, requestHeaders);
    }

    /**
     * Smazání uzlu.
     *
     * @param nodeParam vstupní parametry pro smazání
     */
    @Transactional
    @MessageMapping("/arrangement/levels/delete")
    public void deleteLevel(@Payload final ArrangementController.NodeParam nodeParam,
                            final StompHeaderAccessor requestHeaders) {
        Validate.notNull(nodeParam, "Parametry JP musí být vyplněny");
        Validate.notNull(nodeParam.getVersionId(), "Nebyl vyplněn identifikátor verze AS");
        Validate.notNull(nodeParam.getStaticNode(), "Nebyla zvolena referenční JP");

        ArrNode deleteNode = factoryDO.createNode(nodeParam.getStaticNode());
        ArrNode deleteParent = nodeParam.getStaticNodeParent() == null ? null : factoryDO
                .createNode(nodeParam.getStaticNodeParent());

        ArrFundVersion version = fundVersionRepository.findById(nodeParam.getVersionId())
                .orElseThrow(version(nodeParam.getVersionId()));

        ArrLevel deleteLevel = moveLevelService.deleteLevel(version, deleteNode, deleteParent, false);

        Collection<TreeNodeVO> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(deleteLevel.getNodeParent().getNodeId()),
                               version);
        Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(ArrNodeVO.valueOf(deleteLevel.getNode()), nodeClients.iterator().next());

        // Odeslání dat zpět
		webScoketStompService.sendReceiptAfterCommit(result, requestHeaders);
    }
}
