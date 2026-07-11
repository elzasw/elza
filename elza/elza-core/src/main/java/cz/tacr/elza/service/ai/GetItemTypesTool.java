package cz.tacr.elza.service.ai;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.aiprovider.client.vo.GetItemTypesParams;
import cz.tacr.elza.aiprovider.client.vo.ItemTypeDictionary;
import cz.tacr.elza.aiprovider.client.vo.ItemTypeInfo;
import cz.tacr.elza.aiprovider.client.vo.ItemTypeSpecInfo;
import cz.tacr.elza.aiprovider.client.vo.StandardToolName;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.service.RuleService;

/**
 * Standard {@code getItemTypes} tool — returns a rule set's item-type dictionary
 * so the provider can resolve the bare {@code type}/{@code spec} codes carried in
 * {@code elza.archivalDescription}. Argument/result shapes are defined by the AI
 * provider contract ({@code GetItemTypesParams} / {@code ItemTypeDictionary}).
 */
@Component
public class GetItemTypesTool implements AiTool {

    @Autowired
    private RuleService ruleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public StandardToolName name() {
        return StandardToolName.GET_ITEM_TYPES;
    }

    @Override
    public Object execute(final Object arguments) {
        GetItemTypesParams params = objectMapper.convertValue(arguments, GetItemTypesParams.class);
        if (params == null || params.getRuleSetCode() == null || params.getRuleSetCode().isEmpty()) {
            throw new IllegalArgumentException("getItemTypes requires a non-empty ruleSetCode");
        }
        List<RulItemTypeExt> itemTypes = ruleService.getDescriptionItemTypesByRuleSet(params.getRuleSetCode());
        return new ItemTypeDictionary()
                .ruleSetCode(params.getRuleSetCode())
                .itemTypes(itemTypes.stream().map(this::toItemTypeInfo).toList());
    }

    private ItemTypeInfo toItemTypeInfo(final RulItemTypeExt src) {
        ItemTypeInfo info = new ItemTypeInfo()
                .code(src.getCode())
                .name(src.getName())
                .dataType(src.getDataType().getCode());
        if (src.getRulItemSpecList() != null && !src.getRulItemSpecList().isEmpty()) {
            info.specs(src.getRulItemSpecList().stream()
                    .map(spec -> new ItemTypeSpecInfo().code(spec.getCode()).name(spec.getName()))
                    .toList());
        }
        return info;
    }
}
