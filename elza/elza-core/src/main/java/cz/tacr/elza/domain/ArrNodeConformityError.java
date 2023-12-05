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
 * Pro chybové stavy uzlu {@link ArrNodeConformity} se odkazuje na hodnoty atributů, které jsou ve špatném stavu.
 *
 * @since 19.11.2015
 */
@Entity(name = "arr_node_conformity_error")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformityError {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer nodeConformityErrorId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNodeConformity.class)
    @JoinColumn(name = "nodeConformityId", nullable = false)
    private ArrNodeConformity nodeConformity;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDescItem.class)
    @JoinColumn(name = "desc_item_id", nullable = false)
    private ArrDescItem descItem;

    @Column(name = "desc_item_id", updatable = false, insertable = false)
    private Integer descItemId;

    @Column(length = 1000, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPolicyType.class)
    @JoinColumn(name = "policy_type_id", nullable = true)
    private RulPolicyType policyType;

    @Column(name = "policy_type_id", updatable = false, insertable = false)
    private Integer policyTypeId;

    /**
     * @return id chyby
     */
    public Integer getNodeConformityErrorId() {
        return nodeConformityErrorId;
    }

    /**
     * @param nodeConformityErrorId id chyby
     */
    public void setNodeConformityErrorId(final Integer nodeConformityErrorId) {
        this.nodeConformityErrorId = nodeConformityErrorId;
    }

    /**
     * @return stav uzlu
     */
    public ArrNodeConformity getNodeConformity() {
        return nodeConformity;
    }

    /**
     * @param nodeConformity stav uzlu
     */
    public void setNodeConformity(final ArrNodeConformity nodeConformity) {
        this.nodeConformity = nodeConformity;
    }

    /**
     * @return chybná hodnota atributu
     */
    public ArrDescItem getDescItem() {
        return descItem;
    }

    /**
     * @param descItem chybná hodnota atributu
     */
    public void setDescItem(final ArrDescItem descItem) {
        this.descItem = descItem;
        this.descItemId = (descItem == null) ? null : descItem.getItemId();
    }

    public Integer getDescItemId() {
        return descItemId != null ? descItemId : (descItem != null) ? descItem.getItemId() : null;
    }

    /**
     * @return textový popis chyby
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description textový popis chyby
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
