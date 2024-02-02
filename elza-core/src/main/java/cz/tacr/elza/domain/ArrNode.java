package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;


/**
 * Entita zajišťuje zámek pro uzel kvůli konkurentnímu přístupu.
 *
 * @author Martin Šlapa
 * @since 4. 9. 2015
 */
@Entity(name = "arr_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNode extends AbstractVersionableEntity implements Versionable, Serializable, Comparable<ArrNode> {

    public static final String FIELD_FUND = "fund";
    public static final String FIELD_NODE_ID = "nodeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer nodeId;

    @Column(nullable = true)
    private LocalDateTime lastUpdate;

    @Column(length = StringLength.LENGTH_36, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fund_id", nullable = false)
    private ArrFund fund;

    @Column(name = "fund_id", insertable = false, updatable = false)
    private Integer fundId;

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
        this.fundId = fund == null ? null : fund.getFundId();
    }

    public ArrFund getFund() {
        return fund;
    }

    public Integer getFundId() {
        return fundId;
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
        return Objects.equals(nodeId, other.getNodeId());
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
