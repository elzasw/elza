package cz.tacr.elza.repository.vo;

public class TypeRuleSet {

    private Integer typeId;
    private Integer ruleSetId;

    public TypeRuleSet(final Integer typeId, final Integer ruleSetId) {
        this.typeId = typeId;
        this.ruleSetId = ruleSetId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }
}
