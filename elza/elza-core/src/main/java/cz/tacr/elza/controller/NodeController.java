package cz.tacr.elza.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.NodePlainTextRepresentation;

@RestController
@RequestMapping("/api/v1")
public class NodeController implements NodeApi {

	@Override
	public ResponseEntity<NodePlainTextRepresentation> nodeGetPlainText(Integer fundVersionId, Integer nodeId) {
		// TODO

		return ResponseEntity.ok(new NodePlainTextRepresentation(
				"name_generator", 
				"code_generator",
				"To be, or not to be: that is the question: Whether 'tis nobler in the mind to suffer The slings and arrows of outrageous fortune, Or to take arms against a sea of troubles, And by opposing end them?"));
	}
}
