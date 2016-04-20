package cz.tacr.elza.domain.vo;

import cz.tacr.elza.service.FilterTreeService;


/**
 * Jednoduchý objekt s hodnotou atributu (může obsahovat specifikaci)
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @see FilterTreeService
 * @since 22.03.2016
 */
public class DescItemValue {

    private String value;

    private String specCode;

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


    public static DescItemValue create(final TitleValue titleValue) {
        if (titleValue instanceof UnitdateTitleValue) {
            return new UnitdateDescItemValue(titleValue.getValue(), titleValue.getSpecCode(),
                    ((UnitdateTitleValue) titleValue).getCalendarTypeId());
        } else {
            return new DescItemValue(titleValue.getValue(), titleValue.getSpecCode());
        }
    }
}
