package cz.tacr.elza.controller.vo.nodes.descitems;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;


/**
 * Skupina zapouzdrující typy hodnot atributů pro UI.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
@Deprecated
public class ArrDescItemTypeGroupVO {

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
    private List<RulDescItemTypeExtVO> descItemTypes;

    public ArrDescItemTypeGroupVO() {
    }

    public ArrDescItemTypeGroupVO(final String code) {
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

    public List<RulDescItemTypeExtVO> getDescItemTypes() {
        return descItemTypes;
    }

    public void setDescItemTypes(final List<RulDescItemTypeExtVO> descItemTypes) {
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

        ArrDescItemTypeGroupVO that = (ArrDescItemTypeGroupVO) o;

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
