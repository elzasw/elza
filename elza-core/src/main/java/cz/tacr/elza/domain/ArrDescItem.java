package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "arr_desc_item")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDescItem implements cz.tacr.elza.api.ArrDescItem<ArrChange, RulDescItemType, RulDescItemSpec, ArrNode> {

    @Id
    @GeneratedValue
    private Integer descItemId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;

    @Column(nullable = false)
    private Integer descItemObjectId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "descItemTypeId", nullable = false)
    private RulDescItemType descItemType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemSpec.class)
    @JoinColumn(name = "descItemSpecId", nullable = true)
    private RulDescItemSpec descItemSpec;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

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
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    @Override
    public void setDeleteChange(final ArrChange deleteChange) {
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
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
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
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrDescItem)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        // TODO: zkontrolovat v UI
        if (getDescItemId() == null) {
            return false;
        }

        ArrDescItem other = (ArrDescItem) obj;

        return new EqualsBuilder().append(descItemId, other.getDescItemId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descItemId).append(node.getNodeId()).append(position).toHashCode();
    }
}
