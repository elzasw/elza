package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.NodeItem;
import cz.tacr.elza.controller.vo.SdoItemResult;
import cz.tacr.elza.controller.vo.StructuredObjectItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.StructObjService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class StructureController implements StructureApi {

	@Autowired
	private ArrangementInternalService arrangementInternalService;
	
	@Autowired
    private StructObjService structureService;

	@Autowired
    private ClientFactoryDO factoryDO;

	@Autowired
    private ClientFactoryVO factoryVO;

    /**
     * POST /funds/sdo/{fundId}/item/{structuredObjectId}
     * Create item value of a structured data type
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structure data id (required)
     * @param nodeItem node item (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
	@Transactional
	public ResponseEntity<SdoItemResult> sdoCreateItem(Integer fundId, Integer structuredObjectId, StructuredObjectItem structuredObjectItem) {
		ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
	    ArrStructuredItem structureItem = factoryDO.createStructureItem(structuredObjectItem, structuredObjectItem.getItemTypeId());
	    ArrStructuredItem created = structureService.createStructureItem(structureItem, structuredObjectId, fundVersion.getFundVersionId());

	    SdoItemResult result = new SdoItemResult();
	    result.setItem(factoryVO.createStructuredObjectItem(created));
	    result.setParent(factoryVO.createStructuredObject(created.getStructuredObject()));
	    return ResponseEntity.ok(result);
	}

    /**
     * PUT /funds/sdo/{fundId}/item/{structuredObjectId}/{createNewVersion}
     * Update item value of a structured data type
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structure data id (required)
     * @param createNewVersion create a new version (required)
     * @param structuredObjectItem node item (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<SdoItemResult> sdoUpdateItem(Integer fundId, Integer structuredObjectId, Boolean createNewVersion, StructuredObjectItem structuredObjectItem) {
		ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
        ArrStructuredItem structureItem = factoryDO.createStructureItem(structuredObjectItem);
        ArrStructuredItem updated = structureService.updateStructureItem(structureItem, fundVersion.getFundVersionId(), createNewVersion);

        SdoItemResult result = new SdoItemResult();
	    result.setItem(factoryVO.createStructuredObjectItem(updated));
	    result.setParent(factoryVO.createStructuredObject(updated.getStructuredObject()));
	    return ResponseEntity.ok(result);
	}

    /**
     * DELETE /funds/sdo/{fundId}/item/{structuredObjectId}/{itemObjectId}
     * Delete item value of a structured data type
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structured object id (required)
     * @param itemObjectId item object id (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoDeleteItem(Integer fundId, Integer structuredObjectId, Integer itemObjectId) {
		ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
        structureService.deleteStructureItem(itemObjectId, structuredObjectId, fundVersion.getFundVersionId());

	    return ResponseEntity.ok().build();
	}

    /**
     * DELETE /funds/sdo/{fundId}/item/{structuredObjectId}/by-type/{itemTypeId}
     * Delete items based on the value of a data type structure by attribute type
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structured object id (required)
     * @param itemTypeId item type id (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoDeleteItemsByType(Integer fundId, Integer structuredObjectId, Integer itemTypeId) {
		ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
		structureService.deleteStructureItemsByType(fundVersion.getFundVersionId(), structuredObjectId, itemTypeId);

	    return ResponseEntity.ok().build();
	}
}
