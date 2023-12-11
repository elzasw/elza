package cz.tacr.elza.controller.vo;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.service.FilterTreeService;


/**
 * Obsahuje id uzlu a jeho index v seznamu filtrovaných uzlů.
 *
 * @see FilterTreeService#getFilteredFulltextIds(ArrFundVersion, String)
 * @since 08.04.2016
 */
public class FilterNodePosition {

    /**
     * Id uzlu.
     */
    private Integer nodeId;
    /**
     * Index v seznamu filtrovaných uzlů.
     */
    private Integer index;

    public FilterNodePosition() {

    }

    public FilterNodePosition(final Integer nodeId, final Integer index) {
        this.nodeId = nodeId;
        this.index = index;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(final Integer index) {
        this.index = index;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FilterNodePosition that = (FilterNodePosition) o;

        return new EqualsBuilder()
                .append(nodeId, that.nodeId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(nodeId)
                .toHashCode();
    }
}
