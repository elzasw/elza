package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;

public class ApAttributesInfoVO {

    /**
     * Vyhodnocené typy a specifikace atributů, které jsou třeba pro založení přístupového bodu
     */
    private List<ApCreateTypeVO> attributes = new ArrayList<>();

    /**
     * Chyby při validaci
     */
    private List<String> errors = null;

    public List<ApCreateTypeVO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ApCreateTypeVO> attributes) {
        this.attributes = attributes;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
