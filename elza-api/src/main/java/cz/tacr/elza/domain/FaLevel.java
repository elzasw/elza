package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fa_level")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FaLevel extends EntityBase {

    @Id
    @GeneratedValue
    private Integer faLevelId;

    @Column(nullable = false)
    private Integer nodeId;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer parentNodeId;

    //Zde je chyba hibernate, při použití "referencedColumnName" ignoruje LAZY a vždy načítá celý strom až ke kořenu.
    //Vzhledem k malé hloubce stromu neřešíme.
    //http://stackoverflow.com/questions/14732098/hibernate-fechtype-lazy-not-working-for-composite-manytoone-relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentNodeId", nullable = true, referencedColumnName = "nodeId")
    private FaLevel parentNode;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = FaChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private FaChange createChange;

    @Column(updatable = false, insertable = false, nullable = true)
    private Integer deleteChangeId;

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

    public Integer getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(final Integer parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public FaLevel getParentNode() {
        return parentNode;
    }

    public void setParentNode(final FaLevel parentNode) {
        this.parentNode = parentNode;
        if (parentNode == null) {
            this.parentNodeId = null;
        } else {
            this.parentNodeId = parentNode.getNodeId();
        }
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChangeId(final Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public FaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final FaChange createChange) {
        this.createChange = createChange;
        if (createChange == null) {
            this.createChangeId = null;
        } else {
            this.createChangeId = createChange.getChangeId();
        }
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public void setDeleteChangeId(final Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
    }

    public FaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final FaChange deleteChange) {
        this.deleteChange = deleteChange;
        if (deleteChange == null) {
            this.deleteChangeId = null;
        } else {
            this.deleteChangeId = deleteChange.getChangeId();
        }
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "FaLevel pk=" + faLevelId;
    }
}
