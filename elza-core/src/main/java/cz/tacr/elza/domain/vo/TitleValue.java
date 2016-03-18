package cz.tacr.elza.domain.vo;

/**
 * Popisek hodnoty atributu uzlu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.03.2016
 */
public class TitleValue {

    private String value;

    private String specCode;

    private String iconValue;

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

    public String getIconValue() {
        return iconValue;
    }

    public void setIconValue(String iconValue) {
        this.iconValue = iconValue;
    }
}
