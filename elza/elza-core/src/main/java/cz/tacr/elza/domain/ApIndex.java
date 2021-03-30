package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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

    public void setPart(ApPart part) {
        this.part = part;
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
