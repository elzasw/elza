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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_level")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"position", "parentNodeId", "deleteChangeId"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FaLevel extends EntityBase {

    @Id
    @GeneratedValue
    private Integer faLevelId;

    @Column(nullable = false)
    private Integer nodeId;

    //Zde je chyba hibernate, při použití "referencedColumnName" ignoruje LAZY a vždy načítá celý strom až ke kořenu.
    //Vzhledem k malé hloubce stromu neřešíme.
    //http://stackoverflow.com/questions/14732098/hibernate-fechtype-lazy-not-working-for-composite-manytoone-relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentNodeId", nullable = true, referencedColumnName = "nodeId")
    private FaLevel parentNode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private FaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private FaChange deleteChange;

    @Column(nullable = false)
    private Integer position;

    public Integer getFaLevelId() {
        return faLevelId;
    }

    public void setFaLevelId(final Integer faLevelId) {
        this.faLevelId = faLevelId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public FaLevel getParentNode() {
        return parentNode;
    }

    public void setParentNode(final FaLevel parentNode) {
        this.parentNode = parentNode;
    }

    public FaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final FaChange createChange) {
        this.createChange = createChange;
    }

    public FaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final FaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof FaLevel)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        FaLevel other = (FaLevel) obj;
        return EqualsBuilder.reflectionEquals(this, other, Arrays.asList("parentNode"));
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, Arrays.asList("parentNode"));
    }

    @Override
    public String toString() {
        return "FaLevel pk=" + faLevelId;
    }
}
