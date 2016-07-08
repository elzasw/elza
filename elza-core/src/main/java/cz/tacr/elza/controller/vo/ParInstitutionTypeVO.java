package cz.tacr.elza.controller.vo;

/**
 * VO pro typ instituce.
 *
 * @author Martin Šlapa
 * @since 21.3.2016
 */
public class ParInstitutionTypeVO {

    private Integer id;

    private String code;

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
