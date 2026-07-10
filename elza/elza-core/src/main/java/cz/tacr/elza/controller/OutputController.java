package cz.tacr.elza.controller;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.factory.OutputFactory;
import cz.tacr.elza.controller.vo.OutputFormData;
import cz.tacr.elza.controller.vo.OutputItem;
import cz.tacr.elza.controller.vo.OutputItemRes;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RuleService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class OutputController implements OutputApi {

    private static final Logger logger = LoggerFactory.getLogger(OutputController.class);

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ArrIOService arrIOService;

    @Autowired
	private OutputService outputService;

    @Autowired
	private ArrangementService arrangementService;

    @Autowired
	private ItemTypeRepository itemTypeRepository;

    @Autowired 
    private OutputFactory outputFactory;    

    /**
     * POST /funds/out/item/{outputId}/{fundVersionId}/{outputVersion}/create
     * Create output item
     *
     * @param outputId output id (required)
     * @param fundVersionId fund version id (required)
     * @param outputVersion output version (required)
     * @param outputItem body output item (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputCreateOutputItem(@PathVariable("outputId") Integer outputId,
                                                                @PathVariable("fundVersionId") Integer fundVersionId,
                                                                @PathVariable("outputVersion") Integer outputVersion,
                                                                @Valid @RequestBody OutputItem outputItem) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");

        ArrOutputItem entity = outputFactory.createOutputItem(outputItem);
        ArrOutputItem created = outputService.createOutputItem(entity, outputId, outputVersion, fundVersionId);

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(created.getOutput()),
        		outputFactory.createOutputItem(created)));
    }

    /**
     * PUT /funds/out/item/{fundVersionId}/{outputVersion}/update/{createNewVersion}
     * Update output item
     *
     * @param fundVersionId fund version id (required)
     * @param outputVersion output version (required)
     * @param outputItem body output item (required)
     * @return The request has succeeded. (status code 200)
     */    
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputUpdateOutputItem(@PathVariable("fundVersionId") Integer fundVersionId,
                                                                @PathVariable("outputVersion") Integer outputVersion,
    		                                                    @Valid @RequestBody OutputItem outputItem) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");

        ArrOutputItem entity = outputFactory.createOutputItem(outputItem);
        ArrOutputItem updated = outputService.updateOutputItem(entity, outputVersion, fundVersionId);

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(updated.getOutput()),
        		outputFactory.createOutputItem(updated)));
    }
    
    /**
     * DELETE /funds/out/item/{fundVersionId}/{outputVersion}/delete
     * Delete output item
     *
     * @param fundVersionId fund version id (required)
     * @param outputVersion output version (required)
     * @param body body output item id (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputDeleteOutputItem(@PathVariable("fundVersionId") Integer fundVersionId,
                                                                @PathVariable("outputVersion") Integer outputVersion,
                                                                @PathVariable("outputItemId") Integer outputItemId) {
        ArrOutputItem deleted = outputService.deleteOutputItem(outputItemId, outputVersion, fundVersionId);

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(deleted.getOutput()),
        		outputFactory.createOutputItem(deleted)));    
    }

    /**
     * DELETE /funds/out/item/{fundVersionId}/{outputId}/{outputVersion}/{itemTypeId}
     * Deleting attribute values ​​by type
     *
     * @param fundVersionId fund version id (required)
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param itemTypeId item type id (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputDeleteOutputItemsByType(@PathVariable("fundVersionId") Integer fundVersionId,
    		                                                           @PathVariable("outputId") Integer outputId,
    		                                                           @PathVariable("outputVersion") Integer outputVersion,
    		                                                           @PathVariable("itemTypeId") Integer itemTypeId) {
        ArrOutput output = outputService.deleteOutputItemsByType(fundVersionId, outputId, outputVersion, itemTypeId);

        return ResponseEntity.ok(new OutputItemRes(
                outputFactory.createDef(output),
                null));
    }

    /**
     * PUT /funds/out/item/{fundVersionId}/{outputId}/{outputVersion}/undefined/set
     * Set the attribute to `Undefined`
     *
     * @param fundVersionId fund version id (required)
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param outputItemTypeId output item type id (required)
     * @param outputItemSpecId output item spec id (optional)
     * @param outputItemObjectId output item object id (optional)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputSetNotIdentifiedOutputItem(@PathVariable("fundVersionId") Integer fundVersionId,
                                                                          @PathVariable("outputId") Integer outputId,
                                                                          @PathVariable("outputVersion") Integer outputVersion,
                                                                          @RequestParam(value = "outputItemTypeId", required = true) Integer outputItemTypeId,
                                                                          @RequestParam(value = "outputItemSpecId", required = false) @Nullable Integer outputItemSpecId,
                                                                          @RequestParam(value = "outputItemObjectId", required = false) @Nullable Integer outputItemObjectId) {
        ArrOutputItem oiSet = outputService.setNotIdentifiedDescItem(outputItemTypeId, outputId, outputVersion, fundVersionId, outputItemSpecId, outputItemObjectId);

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(oiSet.getOutput()),
        		outputFactory.createOutputItem(oiSet)));    
    }

    /**
     * PUT /funds/out/item/{fundVersionId}/{outputId}/{outputVersion}/undefined/unset
     * Reset the attribute setting to `Undefined`
     *
     * @param fundVersionId fund version id (required)
     * @param outputVersion output version (required)
     * @param outputItemObjectId output item object id (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputUnsetNotIdentifiedOutputItem(@PathVariable("fundVersionId") Integer fundVersionId,
                                                                            @PathVariable("outputVersion") Integer outputVersion,
                                                                            @PathVariable("outputItemObjectId") Integer outputItemObjectId) {
        ArrOutputItem oiDeleted = outputService.deleteOutputItem(outputItemObjectId, outputVersion, fundVersionId);

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(oiDeleted.getOutput()),
        		null));    
    }

    /**
     * POST /funds/out/item/{fundVersionId}/csv/import
     * Import a CSV file; a new entry will be created with the file&#39;s contents
     *
     * @param fundVersionId fund version id (required)
     * @param outputVersion output version (required)
     * @param outputId output id (required)
     * @param descItemTypeId desc item type id (required)
     * @param file CSV file (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputOutputItemCsvImport(@PathVariable("fundVersionId") Integer fundVersionId,
    		                                                       @RequestPart("outputVersion") Integer outputVersion,
    		                                                       @RequestPart("outputId") Integer outputId,
    		                                                       @RequestPart("descItemTypeId") Integer descItemTypeId,
    		                                                       @RequestPart("file") MultipartFile file) {
        ArrOutputItem created;
        try (InputStream is = file.getInputStream()) {
            created = arrIOService.csvOutputImport(fundVersionId, outputId, outputVersion, descItemTypeId, is);
        } catch (IOException e) {
            throw new SystemException("Chyba při importu CSV", e, BaseCode.IMPORT_FAILED);
        }

        return ResponseEntity.ok(new OutputItemRes(
                outputFactory.createDef(created.getOutput()),
                outputFactory.createOutputItem(created)));
    }

    /**
     * GET /funds/out/item/{fundVersionId}/csv/export
     * Downloading a CSV file from an attribute value
     *
     * @param fundVersionId fund version id (required)
     * @param descItemObjectId desc item object id (required)
     * @return The request has succeeded. (status code 200)
     * @throws IOException 
     */
    @Override
    @Transactional
    public ResponseEntity<Resource> outputOutputItemCsvExport(@PathVariable("fundVersionId") Integer fundVersionId,
    		                                                  @RequestParam(value = "descItemObjectId", required = true) Integer descItemObjectId) {
        ArrOutputItem outputItem = outputService.findOpenOutputItem(descItemObjectId);
        if (!"JSON_TABLE".equals(outputItem.getItemType().getDataType().getCode())) {
            throw new SystemException("Pouze typ JSON_TABLE může být exportován pomocí CSV.", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                    .set("property", "descItemObjectId")
                    .set("expected", "JSON_TABLE")
                    .set("actual", outputItem.getItemType().getDataType().getCode());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            arrIOService.csvExport(outputItem, baos);
        } catch (IOException e) {
            throw new SystemException("Chyba při exportu CSV", e, BaseCode.EXPORT_FAILED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");
        FileDownload.addContentDispositionAsAttachment(headers, "output-item-" + descItemObjectId + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(baos.toByteArray()));
    }

    /**
     * GET /funds/out/{outputId}/{fundVersionId}/form
     * Getting data for the form
     *
     * @param outputId output id (required)
     * @param fundVersionId fund version id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<OutputFormData> outputGetOutputFormData(@PathVariable("outputId") Integer outputId,
                                                                  @PathVariable("fundVersionId") Integer fundVersionId) {
		ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
		ArrOutput output = outputService.getOutput(outputId);
		List<ArrOutputItem> outputItems = outputService.getOutputItems(fundVersion, output);

		List<RulItemTypeExt> itemTypes;
		try {
			itemTypes = ruleService.getOutputItemTypes(output);
		} catch (Exception e) {
			logger.error("Chyba při zpracování pravidel", e);
			itemTypes = new ArrayList<>();
		}

		List<RulItemTypeExt> hiddenItemTypes = outputService.findHiddenItemTypes(fundVersion, output, itemTypes, outputItems);
		
		Integer fundId = fundVersion.getFund().getFundId();
		String ruleCode = fundVersion.getRuleSet().getCode();

        return ResponseEntity.ok(new OutputFormData(
        		outputFactory.createDef(output),
        		outputFactory.createOutputItems(outputItems),
        		outputFactory.createFormItemTypes(ruleCode, fundId, itemTypes),
                hiddenItemTypes.stream().map(RulItemTypeExt::getItemTypeId).collect(toList())));
	}

    /**
     * PUT /funds/out/{outputId}/{fundVersionId}/{itemTypeId}/{manual}
     * Set user-defined/automatic attribute type editing
     *
     * @param outputId output id (required)
     * @param fundVersionId fund version id (required)
     * @param itemTypeId item type id (required)
     * @param manual true/false — manual (user-defined)/automatic (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
    @Transactional
    public ResponseEntity<Void> outputSetOutputItemMode(@PathVariable("outputId") Integer outputId,
                                                           @PathVariable("fundVersionId") Integer fundVersionId,
                                                           @PathVariable("itemTypeId") Integer itemTypeId,
                                                           @PathVariable("manual") Boolean manual) {
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        RulItemType itemType = itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new ObjectNotFoundException("Typ atributu neexistuje", BaseCode.ID_NOT_EXIST).setId(itemTypeId));
        outputService.setOutputItemMode(output, fundVersion, itemType, manual);

        return ResponseEntity.ok().build();
    }
}
