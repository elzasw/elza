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
import javax.persistence.UniqueConstraint;

/**
 * popis {@link cz.tacr.elza.api.ArrLevel}.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_level")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"position", "nodeIdParent", "deleteChangeId"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrLevel implements cz.tacr.elza.api.ArrLevel<ArrChange, ArrNode> {

    @Id
    @GeneratedValue
    private Integer levelId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeIdParent", nullable = true)
    private ArrNode nodeParent;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;

    @Column(nullable = false)
    private Integer position;

    @Override
    public Integer getLevelId() {
        return levelId;
    }

    @Override
    public void setLevelId(final Integer levelId) {
        this.levelId = levelId;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(ArrNode node) {
        this.node = node;
    }

    @Override
    public ArrNode getNodeParent() {
        return nodeParent;
    }

    @Override
    public void setNodeParent(ArrNode parentNode) {
        this.nodeParent = parentNode;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(ArrChange createChange) {
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
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.ArrLevel)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        cz.tacr.elza.domain.ArrLevel other = (cz.tacr.elza.domain.ArrLevel) obj;
        return EqualsBuilder.reflectionEquals(levelId, other.getLevelId());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(levelId);
    }

    @Override
    public String toString() {
        return "ArrLevel pk=" + levelId;
    }
}
