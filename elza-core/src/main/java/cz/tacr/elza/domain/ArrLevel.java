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
 * Úroveň hierarchického popisu. Úroveň sama o sobě není nositelem hodnoty. Vlastní hodnoty prvků
 * popisu jsou zapsány v atributech archivního popisu {@link ArrDescItem}.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_level")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"position", "nodeIdParent", "deleteChangeId"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrLevel {

    @Id
    @GeneratedValue
    private Integer levelId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", insertable = false, updatable = false)
    private Integer nodeId;

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

    public Integer getLevelId() {
        return levelId;
    }

    public void setLevelId(final Integer levelId) {
        this.levelId = levelId;
    }

    /**
    *
    * @return identifikátor uzlu, existuje více záznamů se stejným node_id, všechny reprezentují
    *         stejný uzel stromu - v případě, že je uzel zařazený pouze do jedné AP (má jednoho
    *         nadřízeného), tak je pouze jedno platné node_id - platné = nevyplněné node_id - pokud
    *         je uzel přímo zařazený do jiné AP, tak existují 2 platné záznamy uzlu (nevyplněné
    *         delete_change_id).
    */
    public ArrNode getNode() {
        return node;
    }

    /**
     * identifikátor uzlu, existuje více záznamů se stejným node_id, všechny reprezentují stejný
     * uzel stromu - v případě, že je uzel zařazený pouze do jedné AP (má jednoho nadřízeného), tak
     * je pouze jedno platné node_id - platné = nevyplněné node_id - pokud je uzel přímo zařazený do
     * jiné AP, tak existují 2 platné záznamy uzlu (nevyplněné delete_change_id)
     *
     * @param node identifikátor uzlu.
     */
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    /**
    *
    * @return odkaz na nadřízený uzel stromu, v případě root levelu je NULL.
    */
    public ArrNode getNodeParent() {
        return nodeParent;
    }

    /**
    *
    * @param parentNode odkaz na nadřízený uzel stromu, v případě root levelu je NULL.
    */
    public void setNodeParent(final ArrNode parentNode) {
        this.nodeParent = parentNode;
    }

    /**
     * @return číslo změny vytvoření uzlu.
     */
    public ArrChange getCreateChange() {
        return createChange;
    }

    /**
     * @param createChange číslo změny vytvoření uzlu.
     */
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    /**
     * @return číslo změny smazání uzlu.
     */
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    /**
     * @param deleteChange číslo změny smazání uzlu.
     */
    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    /**
     * @return pozice uzlu mezi sourozenci.
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * @param position pozice uzlu mezi sourozenci.
     */
    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrLevel)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ArrLevel other = (ArrLevel) obj;
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
