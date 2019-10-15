package cz.tacr.elza.controller.vo;

/**
 * VO pro třídu RulPolicyType.
 *
 */
public class RulPolicyTypeVO
        extends BaseCodeVo {

    private Integer ruleSetId;

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
