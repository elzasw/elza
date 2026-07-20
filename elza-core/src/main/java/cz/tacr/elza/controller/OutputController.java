package cz.tacr.elza.controller;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.factory.OutputFactory;
import cz.tacr.elza.controller.vo.OutputDef;
import cz.tacr.elza.controller.vo.OutputFormData;
import cz.tacr.elza.controller.vo.OutputItem;
import cz.tacr.elza.controller.vo.OutputItemRes;
import cz.tacr.elza.controller.vo.OutputNameParam;
import cz.tacr.elza.controller.vo.OutputRequestStatus;
import cz.tacr.elza.controller.vo.OutputRestrictionScope;
import cz.tacr.elza.controller.vo.OutputSettings;
import cz.tacr.elza.controller.vo.OutputState;
import cz.tacr.elza.controller.vo.OutputTemplate;
import cz.tacr.elza.controller.vo.OutputType;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.ArrIOService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.output.OutputData;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1")
public class OutputController implements OutputApi {

    private static final Logger logger = LoggerFactory.getLogger(OutputController.class);

    @Autowired
    private UserService userService;

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
     * POST /funds/out/item/{outputId}/{outputVersion}/create
     * Create output item
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param outputItem body output item (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputCreateOutputItem(@PathVariable("outputId") Integer outputId,
                                                                @PathVariable("outputVersion") Integer outputVersion,
                                                                @Valid @RequestBody OutputItem outputItem) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");

        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutputItem entity = outputFactory.createOutputItem(outputItem);
        ArrOutputItem created = outputService.createOutputItem(entity, outputId, outputVersion, fundVersion.getFundVersionId());

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(created.getOutput()),
        		outputFactory.createOutputItem(created)));
    }

    /**
     * PUT /funds/out/item/{outputId}/{outputVersion}/update/{createNewVersion}
     * Update output item
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param outputItem body output item (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputUpdateOutputItem(@PathVariable("outputId") Integer outputId,
                                                                @PathVariable("outputVersion") Integer outputVersion,
    		                                                    @Valid @RequestBody OutputItem outputItem) {
        Assert.notNull(outputItem, "Výstup musí být vyplněn");

        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutputItem entity = outputFactory.createOutputItem(outputItem);
        ArrOutputItem updated = outputService.updateOutputItem(entity, outputVersion, fundVersion.getFundVersionId());

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(updated.getOutput()),
        		outputFactory.createOutputItem(updated)));
    }

    /**
     * DELETE /funds/out/item/{outputId}/{outputVersion}/delete
     * Delete output item
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param body body output item id (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputDeleteOutputItem(@PathVariable("outputId") Integer outputId,
                                                                @PathVariable("outputVersion") Integer outputVersion,
                                                                @PathVariable("outputItemId") Integer outputItemId) {
        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutputItem deleted = outputService.deleteOutputItem(outputItemId, outputVersion, fundVersion.getFundVersionId());

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(deleted.getOutput()),
        		outputFactory.createOutputItem(deleted)));    
    }

    /**
     * DELETE /funds/out/item/{outputId}/{outputVersion}/by-type/{itemTypeId}
     * Deleting attribute values ​​by type
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param itemTypeId item type id (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputDeleteOutputItemsByType(@PathVariable("outputId") Integer outputId,
    		                                                           @PathVariable("outputVersion") Integer outputVersion,
    		                                                           @PathVariable("itemTypeId") Integer itemTypeId) {
        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutput result = outputService.deleteOutputItemsByType(fundVersion.getFundVersionId(), outputId, outputVersion, itemTypeId);

        return ResponseEntity.ok(new OutputItemRes(
                outputFactory.createDef(result),
                null));
    }

    /**
     * PUT /funds/out/item/{outputId}/{outputVersion}/undefined/set
     * Set the attribute to `Undefined`
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param outputItemTypeId output item type id (required)
     * @param outputItemSpecId output item spec id (optional)
     * @param outputItemObjectId output item object id (optional)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputSetNotIdentifiedOutputItem(@PathVariable("outputId") Integer outputId,
                                                                          @PathVariable("outputVersion") Integer outputVersion,
                                                                          @RequestParam(value = "outputItemTypeId", required = true) Integer outputItemTypeId,
                                                                          @RequestParam(value = "outputItemSpecId", required = false) @Nullable Integer outputItemSpecId,
                                                                          @RequestParam(value = "outputItemObjectId", required = false) @Nullable Integer outputItemObjectId) {
        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutputItem oiSet = outputService.setNotIdentifiedDescItem(
        		outputItemTypeId, outputId, outputVersion, fundVersion.getFundVersionId(), outputItemSpecId, outputItemObjectId);

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(oiSet.getOutput()),
        		outputFactory.createOutputItem(oiSet)));    
    }

    /**
     * PUT /funds/out/item/{outputId}/{outputId}/{outputVersion}/undefined/unset
     * Reset the attribute setting to `Undefined`
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param outputItemObjectId output item object id (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputUnsetNotIdentifiedOutputItem(@PathVariable("outputId") Integer outputId,
                                                                            @PathVariable("outputVersion") Integer outputVersion,
                                                                            @PathVariable("outputItemObjectId") Integer outputItemObjectId) {
        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutputItem oiDeleted = outputService.deleteOutputItem(outputItemObjectId, outputVersion, fundVersion.getFundVersionId());

        return ResponseEntity.ok(new OutputItemRes(
        		outputFactory.createDef(oiDeleted.getOutput()),
        		null));
    }

    /**
     * POST /funds/out/item/{outputId}/csv/import
     * Import a CSV file; a new entry will be created with the file&#39;s contents
     *
     * @param outputId output id (required)
     * @param outputVersion output version (required)
     * @param descItemTypeId desc item type id (required)
     * @param file CSV file (required)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    @Transactional
    public ResponseEntity<OutputItemRes> outputOutputItemCsvImport(@PathVariable("outputId") Integer outputId,
    		                                                       @RequestPart("outputVersion") Integer outputVersion,
    		                                                       @RequestPart("descItemTypeId") Integer descItemTypeId,
    		                                                       @RequestPart("file") MultipartFile file) {
        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

        ArrOutputItem created;
        try (InputStream is = file.getInputStream()) {
            created = arrIOService.csvOutputImport(fundVersion.getFundVersionId(), outputId, outputVersion, descItemTypeId, is);
        } catch (IOException e) {
            throw new SystemException("Chyba při importu CSV", e, BaseCode.IMPORT_FAILED);
        }

        return ResponseEntity.ok(new OutputItemRes(
                outputFactory.createDef(created.getOutput()),
                outputFactory.createOutputItem(created)));
    }

    /**
     * GET /funds/out/item/{outputId}/csv/export
     * Downloading a CSV file from an attribute value
     *
     * @param outputId output id (required)
     * @param descItemObjectId desc item object id (required)
     * @return The request has succeeded. (status code 200)
     * @throws IOException 
     */
    @Override
    @Transactional
    public ResponseEntity<Resource> outputOutputItemCsvExport(@PathVariable("outputId") Integer outputId,
    		                                                  @RequestParam(value = "descItemObjectId", required = true) Integer descItemObjectId) {
        ArrOutput output = outputService.getOutput(outputId);
        // triggers @AuthParam(FUND) permission check
        arrangementService.getOpenVersionByFundId(output.getFundId());

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
     * GET /funds/out/{outputId}/form
     * Getting data for the form
     *
     * @param outputId output id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<OutputFormData> outputGetOutputFormData(@PathVariable("outputId") Integer outputId) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

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
     * @param itemTypeId item type id (required)
     * @param manual true/false — manual (user-defined)/automatic (required)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
    @Transactional
    public ResponseEntity<Void> outputSetOutputItemMode(@PathVariable("outputId") Integer outputId,
                                                        @PathVariable("itemTypeId") Integer itemTypeId,
                                                        @PathVariable("manual") Boolean manual) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());

	    RulItemType itemType = itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new ObjectNotFoundException("Typ atributu neexistuje", BaseCode.ID_NOT_EXIST).setId(itemTypeId));
        outputService.setOutputItemMode(output, fundVersion, itemType, manual);

        return ResponseEntity.ok().build();
    }

    /**
     * GET /funds/out/types/{fundVersionId}
     * Getting output types
     *
     * @param fundVersionId fund version id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<List<OutputType>> outputGetOutputTypes(@PathVariable("fundVersionId") Integer fundVersionId) {
		List<RulOutputType> outputTypes = outputService.getOutputTypes(fundVersionId);

		return ResponseEntity.ok(outputFactory.createOutputTypes(outputTypes));
	}

    /**
     * GET /funds/out/{fundVersionId}
     * Getting list of outputs – an output object linked to a named output object
     *
     * @param fundVersionId fund version id (required)
     * @param state output state (optional)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<List<OutputDef>> outputGetOutputs(@RequestParam("fundVersionId") Integer fundVersionId,
    		                                                @RequestParam(value = "state", required = false) @Nullable OutputState state) {
	    ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
	    List<ArrOutput> outputs = state == null
	            ? outputService.getSortedOutputs(fundVersion)
	            : outputService.getSortedOutputsByState(fundVersion, ArrOutput.OutputState.valueOf(state.name()));

	    return ResponseEntity.ok(outputFactory.createDefList(outputs));		
	}

    /**
     * GET /funds/out/{outputId}
     * Getting output details: an output object linked to a named output, with a list of connected nodes.
     *
     * @param outputId output id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<OutputDef> outputGetOutput(@PathVariable("outputId") Integer outputId) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
	    outputService.getOutput(fundVersion, output);

	    return ResponseEntity.ok(outputFactory.createDefExt(output, fundVersion));		
	}

    /**
     * PUT /funds/out/{outputId}/settings
     * Configuration of generated outputs
     *
     * @param outputId output id (required)
     * @param outputSettings body (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<Void> outputUpdateOutputSettings(@PathVariable("outputId") Integer outputId,
                                                           @Valid @RequestBody OutputSettings outputSettings) {
		try {
	        outputService.setOutputSettings(outputSettings, outputId);
	    } catch (JsonProcessingException e) {
	        throw new SystemException("Chyba serializace nastavení výstupu", e, BaseCode.INVALID_STATE);
	    }
		return ResponseEntity.ok().build();
	}

    /**
     * GET /funds/out/{outputId}/generate
     * Generate output
     *
     * @param outputId output id (required)
     * @param forced forced (required)
     * @return The request has succeeded. (status code 200)
     */	
	@Override
    @Transactional
    public ResponseEntity<OutputRequestStatus> outputGenerateOutput(@PathVariable("outputId") Integer outputId,
                                                                    @NotNull @Valid @RequestParam(value = "forced", defaultValue = "false") Boolean forced) {
	    ArrOutput output = outputService.getOutput(outputId);

	    UsrUser loggedUser = userService.getLoggedUser();
	    Integer userId = loggedUser != null ? loggedUser.getUserId() : null;

	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
	    cz.tacr.elza.service.output.OutputRequestStatus outputRequestStatus = outputService.addRequest(outputId, fundVersion, !forced, userId);

	    return ResponseEntity.ok(OutputRequestStatus.valueOf(outputRequestStatus.name()));		
	}

    /**
     * GET /funds/out/{outputId}/send
     * Send output
     *
     * @param outputId output id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<Void> outputSendOutput(@PathVariable("outputId") Integer outputId) {
        ArrOutput output = outputService.getOutput(outputId);
        outputService.sendOutput(output);

        return ResponseEntity.ok().build();
	}

    /**
     * POST /funds/out/{fundVersionId}
     * Creating a new named output
     *
     * @param fundVersionId fund version id (required)
     * @param outputNameParam output parameters (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<OutputDef> outputCreateNamedOutput(@PathVariable("fundVersionId") Integer fundVersionId,
                                                             @Valid @RequestBody OutputNameParam outputNameParam) {
	    Assert.notNull(outputNameParam, "Vstupní data musí být vyplněny");
	    ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

	    Set<Integer> templateIds = new HashSet<>();
	    if (outputNameParam.getTemplateId() != null) {
	        templateIds.add(outputNameParam.getTemplateId());
	    }
	    if (outputNameParam.getTemplateIds() != null) {
	        templateIds.addAll(outputNameParam.getTemplateIds());
	    }
	    OutputData outputData = outputService.createOutput(fundVersion,
	            outputNameParam.getName(),
	            outputNameParam.getInternalCode(),
	            outputNameParam.getOutputTypeId(),
	            templateIds,
	            outputNameParam.getOutputFilterId());

	    return ResponseEntity.ok(outputFactory.createDefExt(outputData.getOutput(), fundVersion));		
	}

    /**
     * DELETE /funds/out/{outputId}
     * Deleting named output
     *
     * @param outputId output id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<Void> outputDeleteNamedOutput(@PathVariable("outputId") Integer outputId) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
        outputService.deleteNamedOutput(fundVersion, output);

        return ResponseEntity.ok().build();
	}

    /**
     * PUT /funds/out/{outputId}/update
     * Update output
     *
     * @param outputId output id (required)
     * @param outputNameParam output parameters (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<Void> outputUpdateNamedOutput(@PathVariable("outputId") Integer outputId,
                                                        @Valid @RequestBody OutputNameParam param) {
        Assert.notNull(param, "Vstupní data musí být vyplněny");
        ArrOutput output = outputService.getOutput(outputId);
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
        outputService.updateNamedOutput(fundVersion, output, param.getName(), param.getInternalCode(), param.getTemplateId(), param.getAnonymizedAp(), param.getOutputFilterId());

        return ResponseEntity.ok().build();
	}

    /**
     * POST /funds/out/{outputId}/nodes/add
     * Adding nodes to output
     *
     * @param outputId output id (required)
     * @param requestBody list of node ids (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<Void> outputAddNodesNamedOutput(@PathVariable("outputId") Integer outputId,
                                                          @Valid @RequestBody List<Integer> nodeIds) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
        outputService.addNodesNamedOutput(fundVersion, output, nodeIds);

        return ResponseEntity.ok().build();
	}

    /**
     * DELETE /funds/out/{outputId}/nodes/remove
     * Removing nodes from output
     *
     * @param outputId output id (required)
     * @param requestBody list of node ids (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<Void> outputRemoveNodesNamedOutput(@PathVariable("outputId") Integer outputId,
                                                             @Valid @RequestBody List<Integer> nodeIds) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
	    outputService.removeNodesNamedOutput(fundVersion, output, nodeIds);

        return ResponseEntity.ok().build();
	}
 
    /**
     * POST /funds/out/{outputId}/revert
     * Reset status of a named output to `Open`;
     *
     * @param outputId output id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<Void> outputRevertToOpenState(@PathVariable("outputId") Integer outputId) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
	    outputService.revertToOpenState(fundVersion, output);

        return ResponseEntity.ok().build();
    }
    
    /**
     * POST /funds/out/{outputId}/clone
     * Creating copy of output
     *
     * @param outputId output id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
    public ResponseEntity<OutputDef> outputCloneOutput(@PathVariable("outputId") Integer outputId) {
	    ArrOutput output = outputService.getOutput(outputId);
	    ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(output.getFundId());
	    OutputData outputData = outputService.cloneOutput(fundVersion, output);

	    return ResponseEntity.ok(outputFactory.createDefExt(outputData.getOutput(), fundVersion));
    }

    /**
     * POST /funds/out/{outputId}/restrict/{scopeId}
     * Adding limiting register to output
     *
     * @param outputId output id (required)
     * @param scopeId scope id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<OutputRestrictionScope> outputAddRestrictedScope(@PathVariable("outputId") Integer outputId,
                                                                           @PathVariable("scopeId") Integer scopeId) {
		return ResponseEntity.ok(
				outputFactory.createRestrictionScope(outputService.addRestrictedScope(outputId, scopeId)));
	}

    /**
     * DELETE /funds/out/{outputId}/restrict/{scopeId}
     * Deleting limiting register from output
     *
     * @param outputId output id (required)
     * @param scopeId scope id (required)
     * @return The request has succeeded. (status code 200)
     */
	@Override
    @Transactional
	public ResponseEntity<Void> outputDeleteRestrictedScope(@PathVariable("outputId") Integer outputId,
                                                            @PathVariable("scopeId") Integer scopeId) {
		outputService.deleteRestrictedScope(outputId, scopeId);

		return ResponseEntity.ok().build();
	}

    /**
     * POST /funds/out/{outputId}/template/{templateId}
     * Adding template to output
     *
     * @param outputId output id (required)
     * @param templateId template id (required)
     * @return The request has succeeded. (status code 200)
     */
	public ResponseEntity<OutputTemplate> outputAddOutputTemplate(@PathVariable("outputId") Integer outputId,
                                                                  @PathVariable("templateId") Integer templateId) {
	    ArrOutput output = outputService.getOutput(outputId);
	    return ResponseEntity.ok(outputFactory.createTemplate(outputService.addOutputTemplate(output.getFundId(), output, templateId)));		
	}
	
    /**
     * DELETE /funds/out/{outputId}/template/{templateId}
     * Deleting template from output
     *
     * @param outputId output id (required)
     * @param templateId template id (required)
     * @return The request has succeeded. (status code 200)
     */
	public ResponseEntity<Void> outputDeleteOutputTemplate(@PathVariable("outputId") Integer outputId,
                                                            @PathVariable("templateId") Integer templateId) {
    	ArrOutput output = outputService.getOutput(outputId);
    	outputService.deleteOutputTemplate(output.getFundId(), output, templateId);
		
		return ResponseEntity.ok().build();
	}

}
