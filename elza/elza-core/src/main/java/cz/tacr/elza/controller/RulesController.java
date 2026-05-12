package cz.tacr.elza.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.mapper.RulesMapper;
import cz.tacr.elza.controller.vo.ItemTypeList;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.service.RuleService;

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

    private final RuleService ruleService;
    private final RulesMapper mapper;

    @Autowired
    public RulesController(RuleService ruleService, RulesMapper mapper) {
        this.ruleService = ruleService;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<ItemTypeList> rulesListItemTypes(String acceptLanguage) {
        // TODO(localization): Accept-Language is currently accepted but
        //  ignored. rul_item_type / rul_item_spec store name/shortcut/
        //  description in the single language set at package-import time.
        //  A translation layer is required before this header has any effect.
        List<RulItemTypeExt> source = ruleService.getAllDescriptionItemTypes();
        return ResponseEntity.ok(mapper.toItemTypeList(source));
    }
}
