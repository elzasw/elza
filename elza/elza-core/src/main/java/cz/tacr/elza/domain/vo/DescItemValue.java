package cz.tacr.elza.domain.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.service.FilterTreeService;


/**
 * Jednoduchý objekt s hodnotou atributu (může obsahovat specifikaci)
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @see FilterTreeService
 * @since 22.03.2016
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class DescItemValue {

    private String value;

    private String specCode;

    private Integer specId;

    public DescItemValue() {
    }

    public DescItemValue(final String value) {
        this.value = value;
    }

    public DescItemValue(final String value, final String specCode) {
        this.value = value;
        this.specCode = specCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSpecCode() {
        return specCode;
    }

    public void setSpecCode(final String specCode) {
        this.specCode = specCode;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(Integer specId) {
        this.specId = specId;
    }
}
