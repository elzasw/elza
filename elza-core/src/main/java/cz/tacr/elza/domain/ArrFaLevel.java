package cz.tacr.elza.domain;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import cz.req.ax.IdObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_level")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"position", "parentNodeId", "deleteChangeId"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFaLevel implements IdObject<Integer>, cz.tacr.elza.api.ArrFaLevel<ArrFaChange> {

    @Id
    @GeneratedValue
    private Integer faLevelId;

    @Column(nullable = false)
    private Integer nodeId;

    @Column(name = "parentNodeId", nullable = true)
    private Integer parentNodeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrFaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFaChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
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
    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public Integer getParentNodeId() {
        return parentNodeId;
    }

    @Override
    public void setParentNodeId(final Integer parentNodeId) {
        this.parentNodeId = parentNodeId;
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
    @JsonIgnore
    public Integer getId() {
        return faLevelId;
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
        return EqualsBuilder.reflectionEquals(this, other, Arrays.asList("parentNode"));
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, Arrays.asList("parentNode"));
    }

    @Override
    public String toString() {
        return "ArrFaLevel pk=" + faLevelId;
    }
}
