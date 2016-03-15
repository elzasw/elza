package cz.tacr.elza.domain;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.11.2015
 */
@Entity(name = "arr_node_conformity")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformity implements cz.tacr.elza.api.ArrNodeConformity<ArrNode, ArrFundVersion> {

    @Id
    @GeneratedValue
    private Integer nodeConformityId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name = "fundVersionId", nullable = false)
    private ArrFundVersion fundVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = false)
    private State state;

    @Column(length = 1000, nullable = true)
    private String description;

    @Column(nullable = true)
    private Date date;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "nodeConformity", fetch = FetchType.LAZY)
    private Set<ArrNodeConformityError> errorConformity;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "nodeConformity", fetch = FetchType.LAZY)
    private Set<ArrNodeConformityMissing> missingConformity;

    @Override
    public Integer getNodeConformityId() {
        return nodeConformityId;
    }

    @Override
    public void setNodeConformityId(final Integer nodeConformityId) {
        this.nodeConformityId = nodeConformityId;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(final Date date) {
        this.date = date;
    }

    public Set<ArrNodeConformityError> getErrorConformity() {
        return errorConformity;
    }

    public void setErrorConformity(Set<ArrNodeConformityError> errorConformity) {
        this.errorConformity = errorConformity;
    }

    public Set<ArrNodeConformityMissing> getMissingConformity() {
        return missingConformity;
    }

    public void setMissingConformity(Set<ArrNodeConformityMissing> missingConformity) {
        this.missingConformity = missingConformity;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.ArrNodeConformity)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.ArrNodeConformity other = (cz.tacr.elza.domain.ArrNodeConformity) obj;

        return new EqualsBuilder().append(nodeConformityId, other.getNodeConformityId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeConformityId).append(state).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrNodeConformity pk=" + nodeConformityId;
    }
}
