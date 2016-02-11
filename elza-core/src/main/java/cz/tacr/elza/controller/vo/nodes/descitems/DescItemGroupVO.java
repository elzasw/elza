package cz.tacr.elza.controller.vo.nodes.descitems;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.controller.vo.nodes.DescItemTypeDescItemsLiteVO;


/**
 * Skupina zapouzdrující hodnoty atributů pro UI.
 *
 * @author Martin Šlapa
 * @since 11.2.2016
 */
public class DescItemGroupVO {

    /**
     * kód skupiny
     */
    private String code;

    /**
     * seznam typů ve skupině
     */
    private List<DescItemTypeDescItemsLiteVO> types;

    public DescItemGroupVO() {
    }

    public DescItemGroupVO(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public List<DescItemTypeDescItemsLiteVO> getTypes() {
        return types;
    }

    public void setTypes(final List<DescItemTypeDescItemsLiteVO> types) {
        this.types = types;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DescItemGroupVO that = (DescItemGroupVO) o;

        return new EqualsBuilder()
                .append(code, that.code)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(code)
                .toHashCode();
    }
}
