package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.websocket.WebSocketAwareController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private DescriptionItemService descriptionItemService;
    @Autowired
    private ClientFactoryVO factoryVo;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    @MessageMapping("/arrangement/descItems/{fundVersionId}/{nodeVersion}/update/{createNewVersion}")
    public ArrangementController.DescItemResult updateDescItem(
            @Payload final ArrItemVO descItemVO,
            @DestinationVariable(value = "fundVersionId") final Integer fundVersionId,
            @DestinationVariable(value = "nodeVersion") final Integer nodeVersion,
            @DestinationVariable(value = "createNewVersion") final Boolean createNewVersion,
            SimpMessageHeaderAccessor headerAccessor) {

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) headerAccessor.getHeader("simpUser");
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(token);
        SecurityContextHolder.setContext(sc);

        final List<String> receipt = headerAccessor.getNativeHeader("receipt");
        final String receiptId = receipt == null || receipt.isEmpty() ? null : receipt.get(0);

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
        Map sendHeader = new HashMap();
        sendHeader.put("receipt-id", receiptId);
        messagingTemplate.convertAndSend("/topic/api/changes", descItemResult, sendHeader);

        return descItemResult;
    }
}
