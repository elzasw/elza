package cz.tacr.elza.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.SdoBatchUpdateParam;
import cz.tacr.elza.controller.vo.SdoCopyObjectParam;
import cz.tacr.elza.controller.vo.SdoExtensionFund;
import cz.tacr.elza.controller.vo.SdoFindResult;
import cz.tacr.elza.controller.vo.SdoItemResult;
import cz.tacr.elza.controller.vo.SdoType;
import cz.tacr.elza.controller.vo.StructuredObject;
import cz.tacr.elza.controller.vo.StructuredObjectItem;
import cz.tacr.elza.controller.vo.StructuredObjectItems;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StructObjService;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/v1")
public class StructureController implements StructureApi {

	@Autowired
	private ArrangementInternalService arrangementInternalService;

	@Autowired
	private RuleService ruleService;

	@Autowired
    private StructObjService structureService;

	@Autowired
    private ClientFactoryDO factoryDO;

	@Autowired
    private ClientFactoryVO factoryVO;

    /**
     * POST /funds/sdo/{fundId}
     * Creating object of the structured data type
     *
     * @param fundId fund id (required)
     * @param body structured data type code (required)
     * @param value value for the structured data (optional)
     * @return The request has succeeded. (status code 200)
     */
	@Override
	@Transactional
	public ResponseEntity<StructuredObject> sdoCreateObject(Integer fundId, @RequestBody String structureTypeCode, @Nullable String value) {
	    ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
	    RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
	    ArrStructuredObject structuredObject = structureService.createStructObj(fundVersion.getFund(), structureType, ArrStructuredObject.State.TEMP);
	    if (value != null) {
	        structureService.addItemsFromValue(structuredObject, value);
	    }
	    return ResponseEntity.ok(factoryVO.createStructuredObject(structuredObject));
    }

    /**
     * POST /funds/sdo/{fundId}/{structuredObjectId}/copy
     * Creating duplicates of a structured data type and an auto-increment field
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structure data id (required)
     * @param sdoCopyObjectParam batch of data to create (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoCopyObject(Integer fundId, Integer structuredObjectId, @RequestBody SdoCopyObjectParam sdoCopyObjectParam) {
	    Integer count = sdoCopyObjectParam.getCount();
	    Objects.requireNonNull(count, "Počet položek musí být vyplněn");

	    List<Integer> incrementedTypeIds = sdoCopyObjectParam.getIncrementedTypeIds();
	    if (CollectionUtils.isEmpty(incrementedTypeIds)) {
	        throw new IllegalArgumentException("Autoincrementující typ musí být alespoň jeden");
	    }

	    ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
	    ArrStructuredObject structuredObject = structureService.getStructObjById(structuredObjectId);
	    structureService.duplicateStructureDataBatch(fundVersion, structuredObject, count, incrementedTypeIds);

	    return ResponseEntity.ok().build();
    }

    /**
     * POST /funds/sdo/{fundId}/batchUpdate/{structureTypeCode}
     * Bulk update of items/values of a structural type
     *
     * @param fundId fund id (required)
     * @param structureTypeCode structure type code (required)
     * @param sdoBatchUpdateParam batch of data to update (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoUpdateObjects(Integer fundId, String structureTypeCode, @RequestBody SdoBatchUpdateParam sdoBatchUpdateParam) {
	    Objects.requireNonNull(sdoBatchUpdateParam.getAutoincrementItemTypeIds(), "Identifikátory typů atributu pro autoincrement nesmí být null");
	    Objects.requireNonNull(sdoBatchUpdateParam.getDeleteItemTypeIds(), "Identifikátory typů atributu pro odstranění nesmí být null");
	    Objects.requireNonNull(sdoBatchUpdateParam.getItems(), "Položky nesmí být null");
	    if (CollectionUtils.isEmpty(sdoBatchUpdateParam.getIds())) {
	    	throw new IllegalArgumentException("Musí být vyplněn alespoň jeden identifikátor hodnoty strukt. typu");
	    }

	    ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
	    RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);

	    List<ArrStructuredItem> structureItems = sdoBatchUpdateParam.getItems().stream()
	            .map(factoryDO::createStructureItem)
	            .collect(Collectors.toList());

	    structureService.updateStructObjBatch(fundVersion,
	            structureType,
	            sdoBatchUpdateParam.getIds(),
	            structureItems,
	            sdoBatchUpdateParam.getAutoincrementItemTypeIds(),
	            sdoBatchUpdateParam.getDeleteItemTypeIds());

	    return ResponseEntity.ok().build();
    }

    /**
     * POST /funds/sdo/{fundId}/{structuredObjectId}/confirm
     * Confirms the value of a structured data type. Sets the value
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structure data id (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     */
	@Override
	@Transactional
	public ResponseEntity<StructuredObject> sdoConfirm(Integer fundId, Integer structuredObjectId) {
		ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
        ArrStructuredObject structureObject = structureService.getStructObjById(structuredObjectId);
        ArrStructuredObject confirmedStructureObject = structureService.confirmStructureData(fundVersion.getFund(), structureObject);

        return ResponseEntity.ok(factoryVO.createStructuredObject(confirmedStructureObject));
	}

    /**
     * POST /funds/sdo/{fundId}/assignable/{assignable}
     * Assignability settings.
     *
     * @param fundId fund id (required)
     * @param assignable assignable value (required)
     * @param requestBody value ids for a structured data type (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoSetDataAssignable(Integer fundId, Boolean assignable, @RequestBody List<Integer> structureDataIds) {
		ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
        List<ArrStructuredObject> structureDataList = structureService.getStructObjByIds(structureDataIds);
        structureService.setAssignableStructureDataList(fundVersion.getFund(), structureDataList, assignable);

	    return ResponseEntity.ok().build();
	}

    /**
     * GET /funds/sdo/{fundId}/{structuredObjectId}
     * Getting the value of a structured data type
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structure data id (required)
     * @param fundVersionId fund version id (optional)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<StructuredObject> sdoGetObject(Integer fundId, Integer structuredObjectId, @Nullable Integer fundVersionId) {
	    ArrFundVersion fundVersion = fundVersionId != null ? 
	    		arrangementInternalService.getFundVersionById(fundVersionId) : 
	    			arrangementInternalService.getOpenVersionByFundId(fundId);

	    ArrStructuredObject structuredObject = structureService.getStructObjById(structuredObjectId, fundVersion);

        return ResponseEntity.ok(factoryVO.createStructuredObject(structuredObject));
	}

    /**
     * DELETE /funds/sdo/{fundId}
     * Deleting a value(s) of a structured data type
     *
     * @param fundId fund id (required)
     * @param requestBody list of id value(s) of structured data type (required)
     * @param fundVersionId fund version id (optional)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoDeleteObjects(Integer fundId, @RequestBody List<Integer> structureObjectIds, @Nullable Integer fundVersionId) {
	    ArrFundVersion fundVersion = fundVersionId != null ? 
	    		arrangementInternalService.getFundVersionById(fundVersionId) : 
	    			arrangementInternalService.getOpenVersionByFundId(fundId);
        List<ArrStructuredObject> structObjList = structureService.getStructObjByIds(structureObjectIds);
        structureService.deleteStructObj(fundVersion.getFundId(), structObjList);

        return ResponseEntity.ok().build();
	}

    /**
     * GET /funds/sdo/{fundId}/item/{structuredObjectId}
     * GET Getting data for a structured data type form: getFormStructureItems()
     *
     * @param fundId fund id (required)
     * @param structuredObjectId structured object id (required)
     * @param fundVersionId fund version id (optional)
     * @return The request has succeeded. (status code 200)
     */
	@Override
	@Transactional
	public ResponseEntity<StructuredObjectItems> sdoGetFormStructureItems(Integer fundId, Integer structuredObjectId, @Nullable Integer fundVersionId) {
	    ArrFundVersion fundVersion = fundVersionId != null ? 
	    		arrangementInternalService.getFundVersionById(fundVersionId) : 
	    			arrangementInternalService.getOpenVersionByFundId(fundId);

	    ArrStructuredObject structuredObject = structureService.getStructObjById(structuredObjectId);

	    List<ArrStructuredItem> structureItems = structureService.findStructureItems(structuredObject);
	    List<RulItemTypeExt> structureItemTypes = ruleService.getStructureItemTypes(structuredObject.getStructuredTypeId(),
	                                                                                fundVersion, structureItems);

	    String ruleCode = fundVersion.getRuleSet().getCode();

	    StructuredObjectItems result = new StructuredObjectItems();
	    result.setParent(factoryVO.createStructuredObject(structuredObject));
	    result.setItems(structureItems.stream()
	            .map(factoryVO::createStructuredObjectItem)
	            .collect(Collectors.toList()));
	    result.setItemTypes(factoryVO.createFormItemTypes(ruleCode, fundId, structureItemTypes));

	    return ResponseEntity.ok(result);
    }

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
	@Transactional
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

	    ArrFundVersion fundVersion = fundVersionId != null ? 
	    		arrangementInternalService.getFundVersionById(fundVersionId) : 
	    			arrangementInternalService.getOpenVersionByFundId(fundId);

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

    /**
     * GET /funds/sdo/{fundId}/extension/{structureTypeCode}
     * GET Finds available and enabled AS extensions: findFundStructureExtension()
     *
     * @param fundId fund id (required)
     * @param structureTypeCode structure type code (required)
     * @param fundVersionId fund version id (optional)
     * @return The request has succeeded. (status code 200)
     */
	@Override
	@Transactional
    public ResponseEntity<List<SdoExtensionFund>> sdoFindFundStructureExtension(Integer fundId, String structureTypeCode, @Nullable Integer fundVersionId) {
	    ArrFundVersion fundVersion = fundVersionId != null ? 
	    		arrangementInternalService.getFundVersionById(fundVersionId) : 
	    			arrangementInternalService.getOpenVersionByFundId(fundId);

	    RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        List<RulStructuredTypeExtension> allStructureExtensions = structureService.findAllStructureExtensions(structureType);
        List<RulStructuredTypeExtension> structureExtensions = structureService.findStructureExtensions(fundVersion.getFund(), structureType);

        return ResponseEntity.ok(factoryVO.createStructureExtensionFund(allStructureExtensions, structureExtensions));		
	}

    /**
     * PUT /funds/sdo/{fundId}/extension/{structureTypeCode}
     * PUT Sets a specific extension on the AS: setFundStructureExtensions()
     *
     * @param fundId fund id (required)
     * @param structureTypeCode structure type code (required)
     * @param requestBody structure ext codes (required)
     * @return There is no content to send for this request, but the headers may be useful.  (status code 204)
     */
	@Override
	@Transactional
	public ResponseEntity<Void> sdoSetFundStructureExtensions(Integer fundId, String structureTypeCode, @RequestBody List<String> structureExtensionCodes) {
	    ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(fundId);
	    RulStructuredType structureType = structureService.getStructureTypeByCode(structureTypeCode);
        List<RulStructuredTypeExtension> structureExtensions = structureService.findStructureExtensionByCodes(structureExtensionCodes);
        structureService.setFundStructureExtensions(fundVersion, structureType, structureExtensions);

	    return ResponseEntity.ok().build();
	}

    /**
     * GET /funds/sdo/type
     * GET Lists the possible data types that can be used in AS: findStructureTypes()
     *
     * @param fundVersionId fund version id (optional)
     * @return The request has succeeded. (status code 200)
     */	
	@Override
	public ResponseEntity<List<SdoType>> sdoFindStructureTypes(@Nullable Integer fundVersionId) {
		List<RulStructuredType> structuredTypes;
		if (fundVersionId == null) {
			structuredTypes = structureService.findStructureTypes();
		} else {
			ArrFundVersion fundVersion = arrangementInternalService.getFundVersionById(fundVersionId);
			structuredTypes = structureService.findStructureTypes(fundVersion);
		}
		return ResponseEntity.ok(structureService.structuredTypeToSdoType(structuredTypes));
	}

}
