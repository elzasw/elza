package cz.tacr.elza.ui.view;

/**
 * VO pro schválení verze.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 6. 8. 2015
 */
public class VOApproveVersion {

    private Integer arrangementTypeId;

    private Integer ruleSetId;

    public Integer getArrangementTypeId() {
        return arrangementTypeId;
    }

    public void setArrangementTypeId(final Integer arrangementTypeId) {
        this.arrangementTypeId = arrangementTypeId;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
