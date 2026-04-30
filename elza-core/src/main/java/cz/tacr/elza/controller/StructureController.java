package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.NodeItem;
import cz.tacr.elza.controller.vo.StructureItemResult;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.service.StructObjService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class StructureController implements StructureApi {

	@Autowired
    private StructObjService structureService;

	@Autowired
    private ClientFactoryDO factoryDO;

	@Autowired
    private ClientFactoryVO factoryVO;

	/**
     * POST /structure/item/{fundVersionId}/{structureDataId}/{itemTypeId}/create
     * Create item value of a structured data type
     *
     * @param fundVersionId fund version id (required)
     * @param structureDataId structure data id (required)
     * @param itemTypeId item type id (required)
     * @param nodeItem node item (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
	@Transactional
	public ResponseEntity<StructureItemResult> structureCreateItem(Integer fundVersionId, Integer structureDataId, Integer itemTypeId, NodeItem nodeItem) {
	    ArrStructuredItem structureItem = factoryDO.createStructureItem(nodeItem, itemTypeId);
	    ArrStructuredItem created = structureService.createStructureItem(structureItem, structureDataId, fundVersionId);

	    StructureItemResult result = new StructureItemResult();
	    result.setItem(factoryVO.createNodeItem(created));
	    result.setParent(factoryVO.createStructureData(created.getStructuredObject()));
	    return ResponseEntity.ok(result);
	}

    /**
     * PUT /structure/item/{fundVersionId}/create/{createNewVersion}
     * Update item value of a structured data type
     *
     * @param fundVersionId fund version id (required)
     * @param createNewVersion create a new version (required)
     * @param nodeItem node item (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<StructureItemResult> structureUpdateItem(Integer fundVersionId, Boolean createNewVersion, NodeItem nodeItem) {
        ArrStructuredItem structureItem = factoryDO.createStructureItem(nodeItem);
        ArrStructuredItem updated = structureService.updateStructureItem(structureItem, fundVersionId, createNewVersion);

	    StructureItemResult result = new StructureItemResult();
	    result.setItem(factoryVO.createNodeItem(updated));
	    result.setParent(factoryVO.createStructureData(updated.getStructuredObject()));
	    return ResponseEntity.ok(result);
	}

    /**
     * DELETE /structure/item/{fundVersionId}/delete
     * Delete item value of a structured data type
     *
     * @param fundVersionId fund version id (required)
     * @param nodeItem node item (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<StructureItemResult> structureDeleteItem(Integer fundVersionId, NodeItem nodeItem) {
        ArrStructuredItem deleted = structureService.deleteStructureItem(nodeItem.getItemObjectId(), fundVersionId);

        StructureItemResult result = new StructureItemResult();
	    result.setItem(factoryVO.createNodeItem(deleted));
	    result.setParent(factoryVO.createStructureData(deleted.getStructuredObject()));
	    return ResponseEntity.ok(result);
	}

    /**
     * DELETE /structure/item/{fundVersionId}/{structureDataId}/{itemTypeId}
     * Delete items based on the value of a data type structure by attribute type
     *
     * @param fundVersionId fund version id (required)
     * @param structureDataId structure data id (required)
     * @param itemTypeId item type id (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<StructureItemResult> structureDeleteItemsByType(Integer fundVersionId, Integer structureDataId, Integer itemTypeId) {
		ArrStructuredObject structureData = structureService.deleteStructureItemsByType(fundVersionId, structureDataId, itemTypeId);

        StructureItemResult result = new StructureItemResult();
	    result.setItem(null);
	    result.setParent(factoryVO.createStructureData(structureData));
	    return ResponseEntity.ok(result);
	}
}
