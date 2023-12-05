package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

@Entity(name = "ap_rev_index")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApRevIndex {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer indexId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevPart.class)
    @JoinColumn(name = "part_id", nullable = false)
    private ApRevPart part;

    @Column(name = "part_id", nullable = false, updatable = false, insertable = false)
    private Integer partId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String indexType;

    @Column(name = "rev_value",length = StringLength.LENGTH_4000, nullable = false)
    private String revValue;

    public Integer getIndexId() {
        return indexId;
    }

    public void setIndexId(Integer indexId) {
        this.indexId = indexId;
    }

    public ApRevPart getPart() {
        return part;
    }

    public Integer getPartId() {
        return partId;
    }

    public void setPart(ApRevPart part) {
        this.part = part;
        this.partId = part != null ? part.getPartId() : null;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getRevValue() {
        return revValue;
    }

    public void setRevValue(String value) {
        this.revValue = value;
    }

}
