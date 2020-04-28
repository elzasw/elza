package cz.tacr.elza.controller.vo;

import java.util.List;

public class PartValidationErrorsVO {

    /**
     * Identifikátor partu
     */
    private Integer id;

    /**
     * Validační chyby partu
     */
    private List<String> errors = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
