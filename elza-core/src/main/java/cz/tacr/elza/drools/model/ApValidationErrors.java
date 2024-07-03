package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;

public class ApValidationErrors {

    private List<String> errors;

    public ApValidationErrors() {
        this.errors = new ArrayList<>();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    // Add error to the result
    public void addError(String error) {
        // check if not same error twice
        if (!errors.contains(error)) {
            errors.add(error);
        }
    }
}
