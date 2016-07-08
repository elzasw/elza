package cz.tacr.elza.controller.vo.nodes.descitems;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;


/**
 * Skupina zapouzdrující hodnoty atributů pro UI.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
@Deprecated
public class ArrDescItemGroupVO {

    /**
     * kód skupiny
     */
    private String code;

    /**
     * název skupiny
     */
    private String name;

    /**
     * seznam typů ve skupině
     */
    private List<RulDescItemTypeDescItemsVO> descItemTypes;

    public ArrDescItemGroupVO() {
    }

    public ArrDescItemGroupVO(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<RulDescItemTypeDescItemsVO> getDescItemTypes() {
        return descItemTypes;
    }

    public void setDescItemTypes(final List<RulDescItemTypeDescItemsVO> descItemTypes) {
        this.descItemTypes = descItemTypes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrDescItemGroupVO that = (ArrDescItemGroupVO) o;

        return new EqualsBuilder()
                .append(code, that.code)
                .append(name, that.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(code)
                .append(name)
                .toHashCode();
    }
}
