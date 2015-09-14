package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Entita zajišťuje zámek pro uzel kvůli konkurentnímu přístupu.
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Entity(name = "arr_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNode extends AbstractVersionableEntity implements cz.tacr.elza.api.ArrNode {

    @Id
    @GeneratedValue
    private Integer nodeId;

    @Column(nullable = true)
    private LocalDateTime lastUpdate;

    @Override
    public Integer getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrNode)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ArrNode other = (ArrNode) obj;
        return EqualsBuilder.reflectionEquals(nodeId, other.getNodeId());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrNode{" + "nodeId=" + nodeId + ", lastUpdate=" + lastUpdate + '}';
    }
}
