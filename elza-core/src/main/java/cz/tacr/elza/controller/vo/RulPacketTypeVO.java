package cz.tacr.elza.controller.vo;

/**
 * VO pro typ obalu.
 *
 * @author Martin Šlapa
 * @since 18.1.2016
 */
public class RulPacketTypeVO {

    /**
     * identifikátor typu obalu
     */
    private Integer id;

    /**
     * kód
     */
    private String code;

    /**
     * název
     */
    private String name;

    /**
     * identifikátor balíčku
     */
    private Integer packageId;

    /**
     * identifikátor pravidel
     */
    private Integer ruleSetId;

    /**
     * zkratka
     */
    private String shortcut;

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

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(final Integer packageId) {
        this.packageId = packageId;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
