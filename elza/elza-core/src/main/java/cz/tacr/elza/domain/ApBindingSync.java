package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "ap_binding_sync")
public class ApBindingSync {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingSyncId;

    @Column(length = StringLength.LENGTH_36, nullable = false)
    private String lastTransaction;

    @Column(length = StringLength.LENGTH_36)
    private String toTransaction;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId", nullable = false)
    private ApExternalSystem apExternalSystem;

    private Integer page;

    private Integer count;

    public Integer getBindingSyncId() {
        return bindingSyncId;
    }

    public void setBindingSyncId(Integer bindingSyncId) {
        this.bindingSyncId = bindingSyncId;
    }

    public String getLastTransaction() {
        return lastTransaction;
    }

    public void setLastTransaction(String lastTransaction) {
        this.lastTransaction = lastTransaction;
    }

    public String getToTransaction() {
        return toTransaction;
    }

    public void setToTransaction(String toTransaction) {
        this.toTransaction = toTransaction;
    }

    public ApExternalSystem getApExternalSystem() {
        return apExternalSystem;
    }

    public void setApExternalSystem(ApExternalSystem apExternalSystem) {
        this.apExternalSystem = apExternalSystem;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
