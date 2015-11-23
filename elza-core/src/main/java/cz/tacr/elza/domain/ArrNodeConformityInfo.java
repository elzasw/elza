package cz.tacr.elza.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Entity(name = "arr_node_conformity_info")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformityInfo implements cz.tacr.elza.api.ArrNodeConformityInfo<ArrNode, ArrFindingAidVersion> {

    @Id
    @GeneratedValue
    private Integer nodeConformityInfoId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAidVersion.class)
    @JoinColumn(name = "faVersionId", nullable = false)
    private ArrFindingAidVersion faVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = false)
    private State state;

    @Column(length = 1000, nullable = true)
    private String description;

    @Column(nullable = true)
    private Date date;

    @Override
    public Integer getNodeConformityInfoId() {
        return nodeConformityInfoId;
    }

    @Override
    public void setNodeConformityInfoId(final Integer nodeConformityInfoId) {
        this.nodeConformityInfoId = nodeConformityInfoId;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public ArrFindingAidVersion getFaVersion() {
        return faVersion;
    }

    @Override
    public void setFaVersion(final ArrFindingAidVersion faVersion) {
        this.faVersion = faVersion;
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

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrNodeConformityInfo)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrNodeConformityInfo other = (ArrNodeConformityInfo) obj;

        return new EqualsBuilder().append(nodeConformityInfoId, other.getNodeConformityInfoId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeConformityInfoId).append(state).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrNodeConformityInfo pk=" + nodeConformityInfoId;
    }
}
