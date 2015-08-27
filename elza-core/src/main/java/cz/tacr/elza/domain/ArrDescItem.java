package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.req.ax.IdObject;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "arr_desc_item")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDescItem extends AbstractVersionableEntity implements IdObject<Integer>, cz.tacr.elza.api.ArrDescItem<ArrFaChange, RulDescItemType,RulDescItemSpec> {

    @Id
    @GeneratedValue
    private Integer descItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "createFaChangeId", nullable = false)
    private ArrFaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "deleteFaChangeId", nullable = true)
    private ArrFaChange deleteChange;

    @Column(nullable = false)
    private Integer descItemObjectId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "descItemTypeId", nullable = false)
    private RulDescItemType descItemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemSpec.class)
    @JoinColumn(name = "descItemSpecId", nullable = true)
    private RulDescItemSpec descItemSpec;


    @Column(nullable = false)
    private Integer nodeId;

    @Column(nullable = false)
    private Integer position;


    @Override
    public Integer getDescItemId() {
        return descItemId;
    }

    @Override
    public void setDescItemId(final Integer descItemId) {
        this.descItemId = descItemId;
    }

    @Override
    public ArrFaChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrFaChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrFaChange getDeleteChange() {
        return deleteChange;
    }

    @Override
    public void setDeleteChange(final ArrFaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    @Override
    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    @Override
    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    @Override
    public RulDescItemType getDescItemType() {
        return descItemType;
    }

    @Override
    public void setDescItemType(final RulDescItemType descItemType) {
        this.descItemType = descItemType;
    }

    @Override
    public RulDescItemSpec getDescItemSpec() {
        return descItemSpec;
    }

    @Override
    public void setDescItemSpec(final RulDescItemSpec descItemSpec) {
        this.descItemSpec = descItemSpec;
    }

    @Override
    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(final Integer position) {
        this.position = position;
    }


    @Override
    @JsonIgnore
    public Integer getId() {
        return descItemId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrDescItem)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrDescItem other = (ArrDescItem) obj;

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).append(nodeId).append(position).toHashCode();
    }
}
