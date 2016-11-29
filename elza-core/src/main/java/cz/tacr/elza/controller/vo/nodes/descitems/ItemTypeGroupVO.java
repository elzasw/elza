package cz.tacr.elza.controller.vo.nodes.descitems;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;


/**
 * Skupina zapouzdrující typy hodnot atributů pro UI.
 *
 * @author Martin Šlapa
 * @since 11.2.2016
 */
public class ItemTypeGroupVO {

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
    private List<ItemTypeLiteVO> types;

    public ItemTypeGroupVO() {
    }

    public ItemTypeGroupVO(final String code, final String name) {
        this.code = code;
        this.name = name;
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

    public List<ItemTypeLiteVO> getTypes() {
        return types;
    }

    public void setTypes(final List<ItemTypeLiteVO> types) {
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

        ItemTypeGroupVO that = (ItemTypeGroupVO) o;

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
