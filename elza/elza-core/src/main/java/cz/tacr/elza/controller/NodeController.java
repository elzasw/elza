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
	public ResponseEntity<List<FundSearchResult>> nodeSearch(SearchParams searchParams) {
		return ResponseEntity.ok(nodeSearchService.nodeSearch(searchParams));
	}

	// GET /node/search/{fundId}
	@Override
	@Transactional
	public ResponseEntity<List<NodeTreeData>> nodeGetSearchResult(Integer fundId) {
		return ResponseEntity.ok(nodeSearchService.nodeGetSearchResult(fundId));
	}

	// GET /node/plain-text/{fundVersionId}/{nodeId}
	@Override
	@Transactional
	public ResponseEntity<List<NodePlainTextRepresentation>> nodeGetPlainText(Integer fundVersionId, Integer nodeId) {
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
    public ResponseEntity<NodeData> nodeGetNodeData(final @RequestBody NodeDataParam param) {
        return ResponseEntity.ok(levelTreeCacheService.getNodeData(param, userService.getLoggedUserDetail()));
    }
}
