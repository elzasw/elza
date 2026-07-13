package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.mapper.RulesMapper;
import cz.tacr.elza.controller.vo.ItemTypeList;
import cz.tacr.elza.controller.vo.PartType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.StructObjService;

import java.util.List;

/**
 * Public REST API for archival description rules — item types, their
 * specifications and (in the future) data types, rule sets and policy
 * types.
 *
 * Implements the contract generated from {@code elza-openapi.yml} (tag
 * {@code rules}). Endpoints from the legacy {@link RuleController} will be
 * migrated here over time.
 */
@RestController
@RequestMapping("/api/v1")
public class RulesController implements RulesApi {

    private final StructObjService structureService;
    private final RuleService ruleService;
    private final RulesMapper mapper;

    @Autowired
    public RulesController(StructObjService structureService, RuleService ruleService, RulesMapper mapper) {
    	this.structureService = structureService;
        this.ruleService = ruleService;
        this.mapper = mapper;
    }

    /**
     * GET /rules/itemTypes
     * Returns item types together with their specifications and rendering hints;
     * all loaded rule sets by default, or only one when {@code ruleSetCode} is set.
     *
     * @param ruleSetCode When set, return only the item types available in this rule set (`RulRuleSet.code`). (optional)
     * @param acceptLanguage Preferred language for localized strings (e.g. "cs", "en"). (optional)
     * @return The request has succeeded. (status code 200)
     */
    @Override
    public ResponseEntity<ItemTypeList> rulesListItemTypes(String ruleSetCode, String acceptLanguage) {
        // TODO(localization): Accept-Language is currently accepted but
        //  ignored. rul_item_type / rul_item_spec store name/shortcut/
        //  description in the single language set at package-import time.
        //  A translation layer is required before this header has any effect.
        List<RulItemTypeExt> source = ruleService.getDescriptionItemTypesByRuleSet(ruleSetCode);
        return ResponseEntity.ok(mapper.toItemTypeList(source));
    }

    /**
     * GET /rules/partTypes
     * Returns all types of parts.
     *
     * @return The request has succeeded. (status code 200)
     */
    @Override
    public ResponseEntity<List<PartType>> rulesListPartTypes() {
    	List<RulPartType> partTypes = structureService.findPartTypes();
    	List<PartType> result = partTypes.stream()
    			.map(t -> {
    	            PartType pt = new PartType();
    	            pt.setId(t.getPartTypeId());
    	            pt.setCode(t.getCode());
    	            pt.setName(t.getName());
    	            pt.setRepeatable(t.getRepeatable());
    	            pt.setChildPartId(t.getChildPart() != null ? t.getChildPart().getPartTypeId() : null);
    				return pt;
    			})
    			.toList();
    	return ResponseEntity.ok(result);
    }
}
