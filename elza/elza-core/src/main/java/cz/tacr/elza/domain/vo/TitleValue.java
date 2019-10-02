package cz.tacr.elza.domain.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * Popisek hodnoty atributu uzlu.
 *
 * @since 18.03.2016
 */
public class TitleValue extends DescItemValue {


    private String iconValue;

    private Integer position;

    /** Id navázané entity(rejstříkové heslo, osoba, obal...)*/
    private Integer entityId;

    private String specName;

    public TitleValue() {
    }

    public TitleValue(final String value) {
        super(value);
    }

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

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getSpecName() {
        return specName;
    }

    public void setSpecName(String specName) {
        this.specName = specName;
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
