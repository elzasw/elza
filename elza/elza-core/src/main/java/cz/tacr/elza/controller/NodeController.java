package cz.tacr.elza.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.NodeInfo;
import cz.tacr.elza.controller.vo.NodePlainTextRepresentation;
import cz.tacr.elza.controller.vo.NodeSearchResult;
import cz.tacr.elza.controller.vo.NodeTreeData;
import cz.tacr.elza.controller.vo.NodeBase;
import cz.tacr.elza.controller.vo.NodeData;
import cz.tacr.elza.controller.vo.NodeDataParam;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.NodeSearchService;
import cz.tacr.elza.service.UserService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class NodeController implements NodeApi {

	@Autowired
	private ArrangementService arrangementService;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
	private NodeSearchService nodeSearchService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private UserService userService;

    @Autowired
    private NodeRepository nodeRepository;

    // POST /node/search
    @Override
	@Transactional
    // kontrola oprávnění uvnitř metody
	public ResponseEntity<NodeSearchResult> nodeSearch(SearchParams searchParams) {
    	var userDetail = userService.getLoggedUserDetail();
        AuthorizationRequest fundRead = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_RD_ALL);
    	if (fundRead.matches(userDetail)) {
    		return ResponseEntity.ok(nodeSearchService.nodeSearch(searchParams));
    	}
    	// user can read only some funds -> restrict the search to them
        Set<Integer> allowedFundIds = userDetail.getUserPermission().stream()
                .filter(p -> p.getPermission() == Permission.FUND_RD)
                .flatMap(p -> p.getFundIds().stream())
                .collect(Collectors.toSet());
        if (allowedFundIds.isEmpty()) {
            throw new AccessDeniedException("User has no permissions to search for nodes.",
                    userDetail.getUserPermission());
        }
        return ResponseEntity.ok(nodeSearchService.nodeSearch(searchParams, allowedFundIds));
	}

	// GET /node/search/{fundId}
	@Override
	@Transactional
	@AuthMethod(permission = {Permission.ADMIN, Permission.FUND_RD_ALL, Permission.FUND_RD})
	public ResponseEntity<List<NodeTreeData>> nodeGetSearchResult(@AuthParam(type = AuthParam.Type.FUND) Integer fundId) {
		return ResponseEntity.ok(nodeSearchService.nodeGetSearchResult(fundId));
	}

	// GET /node/plain-text/{fundVersionId}/{nodeId}
	@Override
	@Transactional
	@AuthMethod(permission = {Permission.ADMIN, Permission.FUND_RD_ALL, Permission.FUND_RD})
	public ResponseEntity<List<NodePlainTextRepresentation>> nodeGetPlainText(@AuthParam(type = AuthParam.Type.FUND_VERSION) Integer fundVersionId, Integer nodeId) {
		return ResponseEntity.ok(arrangementService.getNodePlainText(fundVersionId, nodeId));
	}

    /**
     * POST /node/node-data
     * Získání dat pro JP.
     *
     * @param param parametry dat, které chceme získat (formálář, sourozence, potomky, předky, ...)
     * @return požadovaná data
     */
	@Override
    @Transactional
    // kontrola oprávnění uvnitř metody služby
    public ResponseEntity<NodeData> nodeGetNodeData(final @RequestBody NodeDataParam param) {
        return ResponseEntity.ok(levelTreeCacheService.getNodeData(param, userService.getLoggedUserDetail()));
    }

    /**
     * PUT /node/copyOlderSiblingAttribute
     * Provede zkopírování atributu daného typu ze staršího bratra uzlu.
     *
     * @param versionId      id verze stromu
     * @param descItemTypeId typ atributu, který chceme zkopírovat
     * @param nodeBase       uzel, na který nastavíme hodnoty ze staršího bratra
     */
	@Override
    @Transactional
	public ResponseEntity<Void> nodeCopyOlderSiblingAttribute(final Integer versionId, final Integer descItemTypeId, final NodeBase nodeBase) {
        arrangementService.copyOlderSiblingAttribute(versionId, descItemTypeId, nodeBase);

		return ResponseEntity.ok().build();
	}

    // GET /node/info/id/{nodeId}
    @Override
    @Transactional
    // permission is checked inside ArrangementService.getNodeInfo (FUND_RD on resolved fundId)
    public ResponseEntity<NodeInfo> nodeGetNodeInfoById(final Integer nodeId) {
        ArrNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ObjectNotFoundException("JP neexistuje", BaseCode.ID_NOT_EXIST).setId(nodeId));
        return ResponseEntity.ok(arrangementService.getNodeInfo(node.getFundId(), node));
    }

    // GET /node/info/uuid/{nodeUuid}
    @Override
    @Transactional
    // permission is checked inside ArrangementService.getNodeInfo (FUND_RD on resolved fundId)
    public ResponseEntity<NodeInfo> nodeGetNodeInfoByUuid(final String nodeUuid) {
        ArrNode node = arrangementInternalService.findNodeByUuid(nodeUuid);
        if (node == null) {
            throw new ObjectNotFoundException("JP neexistuje", BaseCode.ID_NOT_EXIST).setId(nodeUuid);
        }
        return ResponseEntity.ok(arrangementService.getNodeInfo(node.getFundId(), node));
    }

}
