package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;

public class PartValidationErrorsVO {

    /**
     * Identifikátor partu
     */
    final private Integer id;

    /**
     * Validační chyby partu
     */
    final private List<String> errors = new ArrayList<>();

    public PartValidationErrorsVO(final Integer partId) {
        this.id = partId;
    }

    public Integer getId() {
        return id;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        if (errors.contains(error)) {
            return;
        }
        errors.add(error);
    }

    public void addErrors(List<String> errorList) {
        if (errorList != null) {
            for (String error : errorList) {
                addError(error);
            }
        }

    }
}
