package cz.tacr.elza.controller.vo;

/**
 * VO pro třídu RulPolicyType.
 *
 * @author Martin Šlapa
 * @since 29.03.2016
 */
public class RulPolicyTypeVO {

    private Integer id;

    private String code;

    private String name;

    private Integer ruleSetId;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
