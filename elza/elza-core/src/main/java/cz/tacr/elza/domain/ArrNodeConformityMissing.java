package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Textový popis chyby {@link ArrNodeConformity}
 *
 * @since 19.11.2015
 */
@Entity(name = "arr_node_conformity_missing")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformityMissing {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer nodeConformityMissingId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNodeConformity.class)
    @JoinColumn(name = "nodeConformityId", nullable = false)
    private ArrNodeConformity nodeConformity;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "item_type_id", nullable = false)
    private RulItemType itemType;

    @Column(name = "item_type_id", updatable = false, insertable = false)
    private Integer itemTypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "item_spec_id", nullable = true)
    private RulItemSpec itemSpec;

    @Column(name = "item_spec_id", updatable = false, insertable = false)
    private Integer itemSpecId;

    @Column(length = 1000, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policy_type_id", nullable = true)
    private RulPolicyType policyType;

    @Column(name = "policy_type_id", updatable = false, insertable = false)
    private Integer policyTypeId;

    /**
     * @return id textového popisu
     */
    public Integer getNodeConformityMissingId() {
        return nodeConformityMissingId;
    }

    /**
     * @param nodeConformityMissingId id textového popisu
     */
    public void setNodeConformityMissingId(final Integer nodeConformityMissingId) {
        this.nodeConformityMissingId = nodeConformityMissingId;
    }

    /**
     * @return stav uzlu
     */
    public ArrNodeConformity getNodeConformity() {
        return nodeConformity;
    }

    /**
     * @param nodeConformityInfo stav uzlu
     */
    public void setNodeConformity(final ArrNodeConformity nodeConformity) {
        this.nodeConformity = nodeConformity;
    }

    /**
     * @return typ atributu
     */
    public RulItemType getItemType() {
        return itemType;
    }

    /**
     * @param itemType typ atributu
     */
    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
        this.itemTypeId = itemType != null ? itemType.getItemTypeId() : null;
    }

    public Integer getItemTypeId() {
        return itemTypeId != null ? itemTypeId : (itemType == null) ? null : itemType.getItemTypeId();
    }

    /**
     * @return specifikace typu atributu
     */
    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    /**
     * @param itemSpec specifikace typu atributu
     */
    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
        this.itemSpecId = itemSpec == null ? null : itemSpec.getItemSpecId();
    }

    public Integer getItemSpecId() {
        return itemSpecId != null ? itemSpecId : (itemSpec != null) ? itemSpec.getItemSpecId() : null;
    }

    /**
     * @return Textový popis chyby
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Textový popis chyby
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return typy kontrol, validací, archivního popisu
     */
    public RulPolicyType getPolicyType() {
        return policyType;
    }

    /**
     * @param policyType typy kontrol, validací, archivního popisu
     */
    public void setPolicyType(final RulPolicyType policyType) {
        this.policyType = policyType;
        this.policyTypeId = (policyType == null) ? null : policyType.getPolicyTypeId();
    }

    public Integer getPolicyTypeId() {
        return policyTypeId != null ? policyTypeId : (policyType != null) ? policyType.getPolicyTypeId() : null;
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
