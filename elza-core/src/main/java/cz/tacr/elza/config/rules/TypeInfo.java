package cz.tacr.elza.config.rules;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Settings for one description item
 * 
 *
 */
public class TypeInfo {

	/**
	 * Code of description item
	 */
    private String code;

    private Integer width;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TypeInfo typeInfo = (TypeInfo) o;

        return new EqualsBuilder()
                .append(code, typeInfo.code)
                .append(width, typeInfo.width)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(code)
                .append(width)
                .toHashCode();
    }
}