package cz.tacr.elza.domain;

import java.util.Date;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
 * Stav uzlu v rámci verze archivní pomůcky.
 * V případě sdílení jsou stavy uzlů uloženy pro každou verzi AP.
 * Při uzamčení pomůcky zůstane stav uzlu uložen a nemůže již být měněn.
 *
 * @since 19.11.2015
 */
@Entity(name = "arr_node_conformity")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeConformity {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer nodeConformityId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer nodeId;

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

    public ArrNodeConformity() {

    }

    public ArrNodeConformity(final ArrNodeConformity src) {
        this.nodeConformityId = src.nodeConformityId;
        this.node = src.node;
        this.nodeId = src.nodeId;
        this.fundVersion = src.fundVersion;
        this.state = src.state;
        this.description = src.description;
        this.date = src.date;
    }

    /**
     * @return id stavu
     */
    public Integer getNodeConformityId() {
        return nodeConformityId;
    }

    /**
     * @param nodeConformityId id stavu
     */
    public void setNodeConformityId(final Integer nodeConformityId) {
        this.nodeConformityId = nodeConformityId;
    }

    /**
     * @return uzel, kterému názeží stav
     */
    public ArrNode getNode() {
        return node;
    }

    /**
     * @param node uzel, kterému názeží stav
     */
    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node != null ? node.getNodeId() : null;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    /**
     * @return verze archivní pomůcky
     */
    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    /**
     * @param fundVersion verze archivní pomůcky
     */
    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
    }

    /**
     * @return stav uzlu (OK/ERR)
     */
    public State getState() {
        return state;
    }

    /**
     * @param state stav uzlu (OK/ERR)
     */
    public void setState(final State state) {
        this.state = state;
    }

    /**
     * @return textový popis případného chybového stavu
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description textový popis případného chybového stavu
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return datum a čas nastavení stavu
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date datum a čas nastavení stavu
     */
    public void setDate(final Date date) {
        this.date = date;
    }

    public Set<ArrNodeConformityError> getErrorConformity() {
        return errorConformity;
    }

    public void setErrorConformity(final Set<ArrNodeConformityError> errorConformity) {
        this.errorConformity = errorConformity;
    }

    public Set<ArrNodeConformityMissing> getMissingConformity() {
        return missingConformity;
    }

    public void setMissingConformity(final Set<ArrNodeConformityMissing> missingConformity) {
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

    /**
     * Stav uzlu.
     */
    public enum State {
        OK,
        ERR;
    }
}
