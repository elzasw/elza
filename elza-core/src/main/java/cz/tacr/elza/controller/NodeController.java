package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.NodePlainTextRepresentation;
import cz.tacr.elza.controller.vo.NodeSearchResult;
import cz.tacr.elza.controller.vo.FundSearchResult;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.NodeSearchService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class NodeController implements NodeApi {

	@Autowired
	private ArrangementService arrangementService; 

	@Autowired
	private NodeSearchService nodeSearchService;

	// POST /node/search
	@Override
	public ResponseEntity<List<FundSearchResult>> nodeSearch(SearchParams searchParams) {
		return ResponseEntity.ok(nodeSearchService.nodeSearch(searchParams));
	}

	// GET /node/search/{fundId}
	@Override
	public ResponseEntity<List<NodeSearchResult>> nodeGetSearchResult(Integer fundId) {
		return ResponseEntity.ok(nodeSearchService.nodeGetSearchResult(fundId));
	}

	// GET /node/plain-text/{fundVersionId}/{nodeId}
	@Override
	@Transactional
	public ResponseEntity<List<NodePlainTextRepresentation>> nodeGetPlainText(Integer fundVersionId, Integer nodeId) {
		return ResponseEntity.ok(arrangementService.getNodePlainText(fundVersionId, nodeId));
	}
}
