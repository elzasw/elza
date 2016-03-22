package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 19.11.2015
 */
@Entity(name = "arr_node_conformity_missing")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformityMissing implements cz.tacr.elza.api.ArrNodeConformityMissing<ArrNodeConformity
        , RulDescItemType, RulDescItemSpec, RulPolicyType> {

    @Id
    @GeneratedValue
    private Integer nodeConformityMissingId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNodeConformity.class)
    @JoinColumn(name = "nodeConformityId", nullable = false)
    private ArrNodeConformity nodeConformity;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "descItemTypeId", nullable = false)
    private RulDescItemType descItemType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemSpec.class)
    @JoinColumn(name = "descItemSpecId", nullable = true)
    private RulDescItemSpec descItemSpec;

    @Column(length = 1000, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policyTypeId", nullable = true)
    private RulPolicyType policyType;

    @Override
    public Integer getNodeConformityMissingId() {
        return nodeConformityMissingId;
    }

    @Override
    public void setNodeConformityMissingId(final Integer nodeConformityMissingId) {
        this.nodeConformityMissingId = nodeConformityMissingId;
    }

    @Override
    public ArrNodeConformity getNodeConformity() {
        return nodeConformity;
    }

    @Override
    public void setNodeConformity(final ArrNodeConformity nodeConformity) {
        this.nodeConformity = nodeConformity;
    }

    @Override
    public RulDescItemType getDescItemType() {
        return descItemType;
    }

    @Override
    public void setDescItemType(final RulDescItemType descItemType) {
        this.descItemType = descItemType;
    }

    @Override
    public RulDescItemSpec getDescItemSpec() {
        return descItemSpec;
    }

    @Override
    public void setDescItemSpec(final RulDescItemSpec descItemSpec) {
        this.descItemSpec = descItemSpec;
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
    public RulPolicyType getPolicyType() {
        return policyType;
    }

    @Override
    public void setPolicyType(final RulPolicyType policyType) {
        this.policyType = policyType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrNodeConformityMissing)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrNodeConformityMissing other = (ArrNodeConformityMissing) obj;

        return new EqualsBuilder().append(nodeConformityMissingId, other.getNodeConformityMissingId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeConformityMissingId).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrNodeConformityMissing pk=" + nodeConformityMissingId;
    }
}
