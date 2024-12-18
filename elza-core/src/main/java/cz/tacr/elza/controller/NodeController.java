package cz.tacr.elza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.NodePlainTextRepresentation;
import cz.tacr.elza.service.ArrangementService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class NodeController implements NodeApi {

	@Autowired
	private ArrangementService arrangementService; 

	@Override
	@Transactional
	public ResponseEntity<List<NodePlainTextRepresentation>> nodeGetPlainText(Integer fundVersionId, Integer nodeId) {
		return ResponseEntity.ok(arrangementService.getNodePlainText(fundVersionId, nodeId));
	}
}
