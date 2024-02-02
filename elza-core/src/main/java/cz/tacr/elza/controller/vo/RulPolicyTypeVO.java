package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulPolicyType;

/**
 * VO pro třídu RulPolicyType.
 *
 */
public class RulPolicyTypeVO extends BaseCodeVo {

    private Integer ruleSetId;

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

	public static RulPolicyTypeVO newInstance(final RulPolicyType policyType) {
		RulPolicyTypeVO result = new RulPolicyTypeVO();
		result.setId(policyType.getPolicyTypeId());
		result.setName(policyType.getName());
		result.setCode(policyType.getCode());
		result.setRuleSetId(policyType.getRuleSet() != null? policyType.getRuleSet().getRuleSetId() : null);
		return result;
	}
}
