package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * popis {@link cz.tacr.elza.api.ArrFaLevel}.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_level")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"position", "parentNodeId", "deleteFaChangeId"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFaLevel implements cz.tacr.elza.api.ArrFaLevel<ArrFaChange, ArrNode> {

    @Id
    @GeneratedValue
    private Integer faLevelId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "parentNodeId", nullable = true)
    private ArrNode parentNode;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "createFaChangeId", nullable = false)
    private ArrFaChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "deleteFaChangeId", nullable = true)
    private ArrFaChange deleteChange;

    @Column(nullable = false)
    private Integer position;

    @Override
    public Integer getFaLevelId() {
        return faLevelId;
    }

    @Override
    public void setFaLevelId(final Integer faLevelId) {
        this.faLevelId = faLevelId;
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
    public ArrNode getParentNode() {
        return parentNode;
    }

    @Override
    public void setParentNode(ArrNode parentNode) {
        this.parentNode = parentNode;
    }

    @Override
    public ArrFaChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(ArrFaChange createChange) {
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
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrFaLevel)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ArrFaLevel other = (ArrFaLevel) obj;
        return EqualsBuilder.reflectionEquals(faLevelId, other.getFaLevelId());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(faLevelId);
    }

    @Override
    public String toString() {
        return "ArrFaLevel pk=" + faLevelId;
    }
}
