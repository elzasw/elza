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
@Entity(name = "arr_node_conformity_errors")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformityErrors implements cz.tacr.elza.api.ArrNodeConformityErrors<
        ArrNodeConformityInfo, ArrDescItem> {

    @Id
    @GeneratedValue
    private Integer nodeConformityErrorsId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNodeConformityInfo.class)
    @JoinColumn(name = "nodeConformityInfoId", nullable = false)
    private ArrNodeConformityInfo nodeConformityInfo;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDescItem.class)
    @JoinColumn(name = "descItemId", nullable = false)
    private ArrDescItem descItem;

    @Column(length = 1000, nullable = true)
    private String description;

    @Override
    public Integer getNodeConformityErrorsId() {
        return nodeConformityErrorsId;
    }

    @Override
    public void setNodeConformityErrorsId(final Integer nodeConformityErrorsId) {
        this.nodeConformityErrorsId = nodeConformityErrorsId;
    }

    @Override
    public ArrNodeConformityInfo getNodeConformityInfo() {
        return nodeConformityInfo;
    }

    @Override
    public void setNodeConformityInfo(final ArrNodeConformityInfo nodeConformityInfo) {
        this.nodeConformityInfo = nodeConformityInfo;
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
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrNodeConformityErrors)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrNodeConformityErrors other = (ArrNodeConformityErrors) obj;

        return new EqualsBuilder().append(nodeConformityErrorsId, other.getNodeConformityErrorsId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeConformityErrorsId).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrNodeConformityErrors pk=" + nodeConformityErrorsId;
    }
}
