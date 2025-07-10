package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.NodePlainTextRepresentation;
import cz.tacr.elza.controller.vo.NodeTreeData;
import cz.tacr.elza.controller.vo.FundSearchResult;
import cz.tacr.elza.controller.vo.NodeData;
import cz.tacr.elza.controller.vo.NodeDataParam;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.security.AuthorizationRequest;
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
	private NodeSearchService nodeSearchService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private UserService userService;

    // POST /node/search
    @Override
	@Transactional
    // kontrola oprávnění uvnitř metody
	public ResponseEntity<List<FundSearchResult>> nodeSearch(SearchParams searchParams) {
    	var userDetail = userService.getLoggedUserDetail();    
        AuthorizationRequest fundRead = AuthorizationRequest.hasPermission(Permission.ADMIN)
                .or(Permission.FUND_RD_ALL);
    	if (fundRead.matches(userDetail)) {
    		return ResponseEntity.ok(nodeSearchService.nodeSearch(searchParams));    		
    	}
    	// TODO
        // user can read only some funds -> we have to get list of allowed funds 
    	// and pass it to search service    	

		throw new AccessDeniedException("User has no permissions to search for nodes.", userDetail.getUserPermission());
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
     * Získání dat pro JP.
     *
     * @param param parametry dat, které chceme získat (formálář, sourozence, potomky, předky, ...)
     * @return požadovaná data
     */
	// POST /node/node-data
	@Override
    @Transactional
    // kontrola oprávnění uvnitř metody služby
    public ResponseEntity<NodeData> nodeGetNodeData(final @RequestBody NodeDataParam param) {
        return ResponseEntity.ok(levelTreeCacheService.getNodeData(param, userService.getLoggedUserDetail()));
    }
}
