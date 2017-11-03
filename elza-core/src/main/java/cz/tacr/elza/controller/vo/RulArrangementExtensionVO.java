package cz.tacr.elza.controller.vo;

/**
 * VO datového typu {@link cz.tacr.elza.domain.RulArrangementExtension}
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 31.10.2017
 */
public class RulArrangementExtensionVO {

    /**
     * identifikátor
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
}
