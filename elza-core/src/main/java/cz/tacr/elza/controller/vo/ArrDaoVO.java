package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDao;

/**
 * Value objekt {@link ArrDao}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDaoVO {

    private Integer id;

    private Boolean valid;

    private String code;

    private String label;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(final Boolean valid) {
        this.valid = valid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }
}
