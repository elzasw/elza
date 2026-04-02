package cz.tacr.elza.controller;

import static cz.tacr.elza.repository.ExceptionThrow.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.AddLevelParam;
import cz.tacr.elza.controller.vo.ArrInhibitedItemVO;
import cz.tacr.elza.controller.vo.DataString;
import cz.tacr.elza.controller.vo.DataText;
import cz.tacr.elza.controller.vo.ItemData;
import cz.tacr.elza.controller.vo.NodeBase;
import cz.tacr.elza.controller.vo.NodeItem;
import cz.tacr.elza.controller.vo.NodeUpdateItem;
import cz.tacr.elza.controller.vo.UpdateOp;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.nodes.NodeBaseMapper;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrangementFormService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.FundLevelService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.websocket.WebSocketAwareController;
import cz.tacr.elza.websocket.service.WebSoсketStompService;
import jakarta.transaction.Transactional;

/**
 * Kontroler pro zpracování websocket požadavků pro některé kritické modifikace v pořádíní.
 * Jedná se o modifikace, které vyžadují seriové zpracování.
 *
 */
@Controller
@WebSocketAwareController
public class ArrangementWebSocketController {

	public static final String UPDATE_DESC_ITEM_MSG_MAPPING = "/arrangement/descItems/{fundVersionId}/update/{createNewVersion}";

	public static final String UPDATE_DESC_ITEMS_MSG_MAPPING = "/arrangement/descItems/{fundVersionId}/{nodeId}/{nodeVersion}/update/bulk";

	public static final String ADD_LEVEL_MSG_MAPPING = "/arrangement/levels/add";

	public static final String DELETE_LEVEL_MSG_MAPPING = "/arrangement/levels/delete";

	public static final String INHIBIT_INHERITANCE_MSG_MAPPING = "/arrangement/descItems/inhibit";

	public static final String ALLOW_INHERITANCE_MSG_MAPPING = "/arrangement/descItems/allow";

	@Autowired
	private ArrangementFormService arrangementFormService;

	@Autowired
	private ArrangementService arrangementService;

    @Autowired
    private WebSoсketStompService webSoсketStompService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private FundLevelService fundLevelService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @MessageMapping(UPDATE_DESC_ITEM_MSG_MAPPING)
	public void updateDescItem(@Payload final NodeItem nodeItem,
	        	               @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
	                           @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
                               final StompHeaderAccessor requestHeaders) {

		Objects.requireNonNull(nodeItem);
		Objects.requireNonNull(fundVersionId);
		Objects.requireNonNull(createNewVersion);
		Integer nodeId = Objects.requireNonNull(nodeItem.getNodeId());
		Integer nodeVersion = Objects.requireNonNull(nodeItem.getNodeVersion());

		ItemData itemData = nodeItem.getData();

        // nepovolujeme prázdné řádky pro DataText i DataString
    	if (itemData instanceof DataText) {
    		Validate.isTrue(StringUtils.isNotBlank(((DataText) itemData).getTextValue()), "Textové pole nesmí být prázdné");
    	}
    	if (itemData instanceof DataString) {
    		Validate.isTrue(StringUtils.isNotBlank(((DataString) itemData).getStringValue()), "Stringové pole nesmí být prázdné");
    	}

		arrangementFormService.updateDescItem(fundVersionId, nodeId, nodeVersion, nodeItem, createNewVersion, requestHeaders);
    }

    @MessageMapping(UPDATE_DESC_ITEMS_MSG_MAPPING)
    public void updateDescItems(@Payload final NodeUpdateItem[] changeItems,
                                @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
                                @DestinationVariable(value = "nodeId") final Integer nodeId,
                                @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
                                final StompHeaderAccessor requestHeaders) {
        Validate.notEmpty(changeItems);
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(nodeVersion);
        Objects.requireNonNull(fundVersionId);

        arrangementFormService.updateDescItems(fundVersionId, nodeId, nodeVersion, changeItems, requestHeaders);
	}

    /**
     * Přidání uzlu do stromu.
     *
     * @param addLevelParam vstupní parametry
     * @param requestHeaders
     * @return nový přidaný uzel
     */
    @Transactional
    @MessageMapping(ADD_LEVEL_MSG_MAPPING)
    public void addLevel(@Payload final AddLevelParam addLevelParam,
                         final StompHeaderAccessor requestHeaders) {

        Assert.notNull(addLevelParam, "Parametry musí být vyplněny");
        Integer versionId = addLevelParam.getVersionId();
        Assert.notNull(versionId, "Nebyl vyplněn identifikátor verze AS");
        Assert.notNull(addLevelParam.getDirection(), "Směr musí být vyplněn");

        ArrFundVersion version = fundVersionRepository.findById(versionId).orElseThrow(version(versionId));

        ArrNode staticNode = NodeBaseMapper.createEntity(addLevelParam.getStaticNode());
        ArrNode staticParentNode = addLevelParam.getStaticNodeParent() == null ? null : NodeBaseMapper.createEntity(addLevelParam.getStaticNodeParent());

        Set<RulItemType> descItemCopyTypes = new HashSet<>();
        if (CollectionUtils.isNotEmpty(addLevelParam.getDescItemCopyTypes())) {
            descItemCopyTypes.addAll(itemTypeRepository.findAllById(addLevelParam.getDescItemCopyTypes()));
        }

        List<ArrLevel> newLevels = fundLevelService.addNewLevel(version, staticNode, staticParentNode,
                                                         addLevelParam.getDirection(), addLevelParam.getScenarioName(),
                                                         descItemCopyTypes, null, addLevelParam.getCount(), null);
        List<NodeBase> nodes = new ArrayList<>(newLevels.size());
        Collection<TreeNodeVO> nodeClients = null;

        for (ArrLevel newLevel : newLevels) {
            if (CollectionUtils.isNotEmpty(addLevelParam.getCreateItems())) {
                NodeUpdateItem[] changeItems = addLevelParam.getCreateItems().stream()
                		.map(nodeItem -> new NodeUpdateItem().updateOp(UpdateOp.CREATE).item(nodeItem))
                		.toList()
                		.toArray(new NodeUpdateItem[0]);
                Integer fundVersionId = version.getFundVersionId();
                Integer nodeId = newLevel.getNodeId();
                Integer nodeVersion = newLevel.getNode().getVersion();
                arrangementFormService.updateDescItems(fundVersionId, nodeId, nodeVersion, changeItems, null);
            }

            nodes.add(NodeBaseMapper.valueOf(newLevel.getNode()));

            if (nodeClients == null) {
                nodeClients = levelTreeCacheService.getNodesByIds(Collections.singletonList(newLevel.getNodeParent().getNodeId()), version);
                Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
            }
        }

        final ArrangementController.NodesWithParent result = new ArrangementController.NodesWithParent(nodes, nodeClients.iterator().next());

        // Odeslání dat zpět
        webSoсketStompService.sendReceiptAfterCommit(result, requestHeaders);
    }

    /**
     * Smazání uzlu.
     *
     * @param nodeParam vstupní parametry pro smazání
     */
    @Transactional
    @MessageMapping(DELETE_LEVEL_MSG_MAPPING)
    public void deleteLevel(@Payload final ArrangementController.NodeParam nodeParam,
                            final StompHeaderAccessor requestHeaders) {
        Validate.notNull(nodeParam, "Parametry JP musí být vyplněny");
        Validate.notNull(nodeParam.getVersionId(), "Nebyl vyplněn identifikátor verze AS");
        Validate.notNull(nodeParam.getStaticNode(), "Nebyla zvolena referenční JP");

        ArrNode deleteNode = NodeBaseMapper.createEntity(nodeParam.getStaticNode());
        ArrNode deleteParent = nodeParam.getStaticNodeParent() == null ? null : NodeBaseMapper.createEntity(nodeParam.getStaticNodeParent());

        ArrFundVersion version = fundVersionRepository.findById(nodeParam.getVersionId())
                .orElseThrow(version(nodeParam.getVersionId()));

        ArrLevel deleteLevel = fundLevelService.deleteLevel(version, deleteNode, deleteParent, false);

        Collection<TreeNodeVO> nodeClients = levelTreeCacheService
                .getNodesByIds(Arrays.asList(deleteLevel.getNodeParent().getNodeId()),
                               version);
        Assert.notEmpty(nodeClients, "Kolekce JP nesmí být prázdná");
        final ArrangementController.NodeWithParent result = new ArrangementController.NodeWithParent(NodeBaseMapper.valueOf(deleteLevel.getNode()), nodeClients.iterator().next());

        // odeslání dat zpět
		webSoсketStompService.sendReceiptAfterCommit(result, requestHeaders);
    }

    /**
     * Potlačení dědictví item.
     * 
     * @param ArrInhibitedItemVO obsahuje nodeId & descItemObjectId
     * @param requestHeaders
     */
    @Transactional
    @MessageMapping(INHIBIT_INHERITANCE_MSG_MAPPING)
    public void inhibitItem(@Payload final ArrInhibitedItemVO arrInhibitedItem, final StompHeaderAccessor requestHeaders) {
        Objects.requireNonNull(arrInhibitedItem);
        Objects.requireNonNull(arrInhibitedItem.getNodeId());
        Objects.requireNonNull(arrInhibitedItem.getDescItemObjectId());

        // pro kontrolu oprávnění ve servisu
        ArrNode node = arrangementService.getNode(arrInhibitedItem.getNodeId());

        Integer inhibitItemId = arrangementService.inhibitItem(node, arrInhibitedItem.getDescItemObjectId());
		webSoсketStompService.sendReceiptAfterCommit(inhibitItemId, requestHeaders);
    }

    /**
     * Povolení dědictví item.
     * 
     * @param ArrInhibitedItemVO obsahuje nodeId & descItemObjectId
     * @param requestHeaders
     */
    @Transactional
    @MessageMapping(ALLOW_INHERITANCE_MSG_MAPPING)
    public void allowItem(@Payload final ArrInhibitedItemVO arrInhibitedItem, final StompHeaderAccessor requestHeaders) {
        Objects.requireNonNull(arrInhibitedItem);
        Objects.requireNonNull(arrInhibitedItem.getNodeId());
        Objects.requireNonNull(arrInhibitedItem.getDescItemObjectId());

        ArrInhibitedItem inhibitedItem = arrangementService.getInhibitedItem(arrInhibitedItem.getNodeId(), arrInhibitedItem.getDescItemObjectId()); 

        Integer resultItemId = arrangementService.allowItem(inhibitedItem.getNode(), inhibitedItem);
		webSoсketStompService.sendReceiptAfterCommit(resultItemId, requestHeaders);
    }
}
