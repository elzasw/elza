package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;


/**
 * Entita zajišťuje zámek pro uzel kvůli konkurentnímu přístupu.
 *
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Entity(name = "arr_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNode extends AbstractVersionableEntity implements Versionable, Serializable, Comparable<ArrNode> {

    public static final String FUND = "fund";
    public static final String NODE_ID = "nodeId";

    @Id
    @GeneratedValue
    private Integer nodeId;

    @Column(nullable = true)
    private LocalDateTime lastUpdate;

    @Column(length = StringLength.LENGTH_36, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private List<UIVisiblePolicy> policies;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    private List<ArrLevel> levels;

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    public ArrFund getFund() {
        return fund;
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
        return "ArrNode{" + "nodeId=" + nodeId + ", lastUpdate=" + lastUpdate + "uuid=" + uuid + '}';
    }

    public List<UIVisiblePolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(final List<UIVisiblePolicy> policies) {
        this.policies = policies;
    }

    public List<ArrLevel> getLevels() {
        return levels;
    }

    public void setLevels(final List<ArrLevel> levels) {
        this.levels = levels;
    }

    @Override
    public int compareTo(final ArrNode o) {
        return getNodeId().compareTo(o.getNodeId());
    }
}
