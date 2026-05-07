package cz.tacr.elza.controller;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.SdoFindResult;
import cz.tacr.elza.controller.vo.SdoItemResult;
import cz.tacr.elza.controller.vo.StructuredObjectItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FilteredResult;
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

    /**
     * GET /funds/sdo/{fundId}/search/{structureTypeCode}
     * GET Searching for values of a structured data type: findStructObj()
     *
     * @param fundId fund id (required)
     * @param structureTypeCode structure type code (required)
     * @param search text for filtering (optional) (optional)
     * @param assignable assignable value (optional)
     * @param from from default &#x3D; 0 (optional, default to 0)
     * @param count max number of items (optional, default to 200)
     * @param fundVersionId fund version id (optional)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    public ResponseEntity<SdoFindResult> sdoFindStructObj(Integer fundId,
                                                          String structureTypeCode,
                                                          @Nullable String search,
                                                          @Nullable Boolean assignable,
                                                          Integer from,
                                                          Integer count,
                                                          @Nullable Integer fundVersionId) {
	    if (from < 0) {
	        throw new SystemException("Hodnota nesmí být záporná", 
	        		BaseCode.PROPERTY_IS_INVALID)
	                .set("from", from);
	    }
	    if (count <= 0) {
	        throw new SystemException("Hodnota musí být kladná", 
	        		BaseCode.PROPERTY_IS_INVALID)
	                .set("count", count);
	    }

	    ArrFundVersion fundVersion;
	    if (fundVersionId != null) {
	        fundVersion = arrangementInternalService.getFundVersionById(fundVersionId);
	    } else {
	        fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
	    }

	    RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
	    FilteredResult<ArrStructuredObject> filteredResult = structureService.findStructureData(
	            structureType, fundVersion.getFund(), search, assignable, from, count);

	    SdoFindResult result = new SdoFindResult();
	    result.setCount((long) filteredResult.getTotalCount());
	    result.setRows(filteredResult.getList().stream()
	            .map(factoryVO::createStructuredObject)
	            .collect(Collectors.toList()));

	    return ResponseEntity.ok(result);
    }
}
