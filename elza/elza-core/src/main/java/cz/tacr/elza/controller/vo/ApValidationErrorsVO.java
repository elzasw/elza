package cz.tacr.elza.controller.vo;

import jakarta.annotation.Nullable;
import java.util.List;

public class ApValidationErrorsVO {

    /**
     * Validační chyby entity
     */
    @Nullable
    private List<String> errors = null;

    /**
     * Validační chyby partů
     */
    @Nullable
    private List<PartValidationErrorsVO> partErrors = null;

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<PartValidationErrorsVO> getPartErrors() {
        return partErrors;
    }

    public void setPartErrors(List<PartValidationErrorsVO> partErrors) {
        this.partErrors = partErrors;
    }
}
