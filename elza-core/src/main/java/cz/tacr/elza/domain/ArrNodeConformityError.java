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
@Entity(name = "arr_node_conformity_error")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformityError implements cz.tacr.elza.api.ArrNodeConformityError<
        ArrNodeConformity, ArrDescItem, RulPolicyType> {

    @Id
    @GeneratedValue
    private Integer nodeConformityErrorId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNodeConformity.class)
    @JoinColumn(name = "nodeConformityId", nullable = false)
    private ArrNodeConformity nodeConformity;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDescItem.class)
    @JoinColumn(name = "descItemId", nullable = false)
    private ArrDescItem descItem;

    @Column(length = 1000, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policyTypeId", nullable = false)
    private RulPolicyType policyType;

    @Override
    public Integer getNodeConformityErrorId() {
        return nodeConformityErrorId;
    }

    @Override
    public void setNodeConformityErrorId(final Integer nodeConformityErrorId) {
        this.nodeConformityErrorId = nodeConformityErrorId;
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
    public ArrDescItem getDescItem() {
        return descItem;
    }

    @Override
    public void setDescItem(final ArrDescItem descItem) {
        this.descItem = descItem;
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
        if (!(obj instanceof cz.tacr.elza.domain.ArrNodeConformityError)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.ArrNodeConformityError other = (cz.tacr.elza.domain.ArrNodeConformityError) obj;

        return new EqualsBuilder().append(nodeConformityErrorId, other.getNodeConformityErrorId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeConformityErrorId).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrNodeConformityError pk=" + nodeConformityErrorId;
    }
}
