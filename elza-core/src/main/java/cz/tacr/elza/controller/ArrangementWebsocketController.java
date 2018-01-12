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
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.ArrangementFormService;
import cz.tacr.elza.service.LevelTreeCacheService;
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

	@MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}")
	public void updateDescItem(
	        @Payload final ArrItemVO descItemVO,
	        @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
	        @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
	        @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
	        final StompHeaderAccessor requestHeaders) {

		Validate.notNull(fundVersionId);
		Validate.notNull(nodeVersion);
		Validate.notNull(descItemVO);

		// why is it here?
		UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) requestHeaders
		        .getHeader("simpUser");
		SecurityContext sc = new SecurityContextImpl();
		sc.setAuthentication(token);
		SecurityContextHolder.setContext(sc);

		arrangementFormService.updateDescItem(fundVersionId, nodeVersion, descItemVO,
		        BooleanUtils.isNotFalse(createNewVersion),
		        requestHeaders);
	}

    /**
     * Přidání uzlu do stromu.
     *
     * @param addLevelParam vstupní parametry
     * @return nový přidaný uzel
     */
    @Transactional
    @MessageMapping("/arrangement/levels/add")
    public void addLevel(@Payload final ArrangementController.AddLevelParam addLevelParam,
                         final StompHeaderAccessor requestHeaders) {

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
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(ArrNodeVO.valueOf(newLevel.getNode()), nodeClients.iterator().next());

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
    public void deleteLevel(@Payload final ArrangementController.NodeParam nodeParam, final StompHeaderAccessor requestHeaders) {
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
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(ArrNodeVO.valueOf(deleteLevel.getNode()), nodeClients.iterator().next());

        // Odeslání dat zpět
		webScoketStompService.sendReceiptAfterCommit(result, requestHeaders);
    }
}
