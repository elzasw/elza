package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
 * Textový popis chyby {@link ArrNodeConformity}
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
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
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId", nullable = true)
    private RulItemSpec descItemSpec;

    @Column(length = 1000, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policyTypeId", nullable = true)
    private RulPolicyType policyType;

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
    }

    /**
     * @return specifikace typu atributu
     */
    public RulItemSpec getItemSpec() {
        return descItemSpec;
    }

    /**
     * @param itemSpec specifikace typu atributu
     */
    public void setItemSpec(final RulItemSpec itemSpec) {
        this.descItemSpec = itemSpec;
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
