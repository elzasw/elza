package cz.tacr.elza.domain.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * Popisek hodnoty atributu uzlu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.03.2016
 */
public class TitleValue extends DescItemValue {


    private String iconValue;

    private Integer position;


    public String getIconValue() {
        return iconValue;
    }

    public void setIconValue(String iconValue) {
        this.iconValue = iconValue;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TitleValue)) {
            return false;
        }

        TitleValue that = (TitleValue) o;

        return new EqualsBuilder()
                .append(position, that.position)
                .append(getValue(), that.getValue())
                .append(getSpecCode(), that.getSpecCode())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(position)
                .append(getValue())
                .toHashCode();
    }
}
