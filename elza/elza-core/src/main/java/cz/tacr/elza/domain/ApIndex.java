package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(name = "ap_index")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApIndex implements AccessPointCacheSerializable {

    public static final String VALUE = "value";
    public static final String INDEX_TYPE = "indexType";
    public static final String PART = "part";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer indexId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "part_id", nullable = false)
    private ApPart part;

    @Column(name = "part_id", nullable = false, updatable = false, insertable = false)
    private Integer partId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String indexType;

    @Column(length = StringLength.LENGTH_4000, nullable = false)
    private String value;

    public Integer getIndexId() {
        return indexId;
    }

    public void setIndexId(Integer indexId) {
        this.indexId = indexId;
    }

    public ApPart getPart() {
        return part;
    }

    public Integer getPartId() {
        return partId;
    }

    public void setPart(ApPart part) {
        this.part = part;
        this.partId = part != null ? part.getPartId() : null;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
