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
}
