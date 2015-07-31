package cz.tacr.elza.ui.view;


/**
 * Value object pro formulář na vytvoření nové archivní pomůcky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
public class VONewFindingAid {

    private String name;

    private Integer arrangementTypeId;

    private Integer ruleSetId;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

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
