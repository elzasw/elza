package cz.tacr.elza.controller;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ItemTypeRepository;
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
     * GET /output/{outputId}/{fundVersionId}/form
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
     * POST /output/{outputId}/{fundVersionId}/{itemTypeId}/switch
     * Switch between automatic and user-defined attribute type editing
     *
     * @param outputId output id (required)
     * @param fundVersionId fund version id (required)
     * @param itemTypeId item type id (required)
     * @param strict fund version id (optional)
     * @return The request has succeeded. (status code 200)
     *         or The server cannot find the requested resource. (status code 404)
     */
	@Override
    @Transactional
    public ResponseEntity<Boolean> outputSwitchOutputCalculating(@PathVariable("outputId") Integer outputId,
                                                                 @PathVariable("fundVersionId") Integer fundVersionId,
                                                                 @PathVariable("itemTypeId") Integer itemTypeId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
        ArrOutput output = outputService.getOutput(outputId);
        RulItemType itemType = itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new ObjectNotFoundException("Typ atributu neexistuje", BaseCode.ID_NOT_EXIST).setId(itemTypeId));

        // FIXME strict was a query param in the legacy endpoint (default false) — see issue #9902
        return ResponseEntity.ok(outputService.switchOutputCalculating(output, fundVersion, itemType, true));
    }
}
