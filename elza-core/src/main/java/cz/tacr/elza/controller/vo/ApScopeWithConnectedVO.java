package cz.tacr.elza.controller.vo;

import cz.tacr.elza.core.data.RuleSet;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysLanguage;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Třída rejstříku doplněná o na ni navázané rejstříky.
 *
 * @since 03.07.2019
 */
public class ApScopeWithConnectedVO extends ApScopeVO {

    private List<ApScopeVO> connectedScopes;

    public List<ApScopeVO> getConnectedScopes() {
        return connectedScopes;
    }

    public void setConnectedScopes(List<ApScopeVO> connectedScopes) {
        this.connectedScopes = connectedScopes;
    }

    public static ApScopeWithConnectedVO newInstance(ApScope src, StaticDataProvider staticData, List<ApScope> connectedScopes) {
        final ApScopeWithConnectedVO vo = new ApScopeWithConnectedVO();
        vo.setCode(src.getCode());
        vo.setId(src.getScopeId());
        vo.setName(src.getName());
        if (src.getLanguageId() != null) {
            SysLanguage lang = staticData.getSysLanguageById(src.getLanguageId());
            vo.setLanguage(lang.getCode());
        }
        if (src.getRulRuleSet() != null) {
            RuleSet ruleSet = staticData.getRuleSetById(src.getRuleSetId());
            vo.setRuleSetCode(ruleSet.getCode());
        }
        if (CollectionUtils.isNotEmpty(connectedScopes)) {
            vo.connectedScopes = connectedScopes.stream().map(s -> ApScopeVO.newInstance(s, staticData)).collect(Collectors.toList());
        }
        return vo;
    }

}
